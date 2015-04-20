/*
 * Copyright 2013-2015 Mikhail Shugay (mikhail.shugay@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last modified on 17.3.2015 by mikesh
 */

package com.milaboratory.oncomigec.pipeline.analysis;

import com.milaboratory.oncomigec.core.PipelineBlock;
import com.milaboratory.oncomigec.core.genomic.Reference;
import com.milaboratory.oncomigec.core.genomic.ReferenceLibrary;
import com.milaboratory.oncomigec.core.mapping.*;
import com.milaboratory.oncomigec.core.mapping.alignment.AlignerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class PipelineConsensusAlignerFactory extends PipelineBlock {
    private final Map<Sample, ConsensusAligner> alignersBySample = new HashMap<>();
    private final List<Reference> references;
    private final PConsensusAlignerFactory pairedFactory;
    private final SConsensusAlignerFactory singleFactory;

    public PipelineConsensusAlignerFactory(AlignerFactory alignerFactory,
                                           ConsensusAlignerParameters consensusAlignerParameters) {
        super("mapping");
        this.pairedFactory = new PConsensusAlignerFactory(alignerFactory, consensusAlignerParameters);
        this.singleFactory = new SConsensusAlignerFactory(alignerFactory, consensusAlignerParameters);
        ReferenceLibrary referenceLibrary = alignerFactory.getReferenceLibrary();
        this.references = new ArrayList<>(referenceLibrary.getReferences());

        Collections.sort(references, new Comparator<Reference>() {
            @Override
            public int compare(Reference o1, Reference o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    public ConsensusAligner create(Sample sample) {
        ConsensusAligner aligner = sample.isPairedEnd() ? pairedFactory.create() : singleFactory.create();
        alignersBySample.put(sample, aligner);
        return aligner;
    }

    @Override
    public String getHeader() {
        List<String> referenceNames = new ArrayList<>();

        for (Reference reference : references)
            referenceNames.add(reference.getName());

        return "sample.group\tsample\t" +
                "migs.good.alignment\tmigs.aligned\t" +
                "migs.chimeric\tmigs.skipped\tmigs.total" +
                StringUtils.join(referenceNames, "\t");
    }

    @Override
    public String getBody() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Sample sample : alignersBySample.keySet()) {
            ConsensusAligner aligner = alignersBySample.get(sample);
            stringBuilder.append(sample.getParent().getName()).append("\t").
                    append(sample.getName()).append("\t").
                    append(aligner.getGoodAlignmentMigs()).append("\t").
                    append(aligner.getAlignedMigs()).append("\t").
                    append(aligner.getChimericMigs()).append("\t").
                    append(aligner.getSkippedMigs()).append("\t").
                    append(aligner.getTotalMigs());

            for (Reference reference : references) {
                MutationsTable mutationsTable = aligner.getAlignerTable(reference);
                stringBuilder.append("\t").append(mutationsTable.getMigCount());
            }

            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }
}
