package com.fs.doc.emr;


import com.fs.doc.emr.extractor.ExtractorUtils;
import com.fs.doc.emr.extractor.ValueType;
import com.google.common.base.Strings;
import com.google.common.collect.*;

import java.util.*;

public class JigsawEmr {
    public JigsawResult parseDocument(EmrTemplate template, String doc) {
        Map<String, EmrLabel> labelMap = template.getRootLabelMap();
        String preProcessed = preProcessDocument(doc);

        JigsawResult jigsawResult = new JigsawResult();
        jigsawResult.recordDocLength(doc.length());
        parseDocumentParts(labelMap, preProcessed, jigsawResult);

        return jigsawResult;
    }

    private void parseDocumentParts(Map<String, EmrLabel> labelMap, String doc, JigsawResult result) {
        RangeMap<Integer, String> rangeMap = TreeRangeMap.create();

        Map<String, EmrLabel> titleToLabel = Maps.newHashMap();
        List<String> allTitles = Lists.newArrayList();
        for (Map.Entry<String, EmrLabel> entry : labelMap.entrySet()) {
            EmrLabel label = entry.getValue();
            List<String> titles = label.getOptionalTitles();
            for (String title : titles) {
                titleToLabel.put(title, label);
            }

            allTitles.addAll(titles);
        }

        Collections.sort(allTitles, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                if (o1.startsWith("*") && o2.startsWith("*")) {
                    String cleanO1 = o1.substring(1);
                    String cleanO2 = o2.substring(1);

                    if (cleanO1.length() == 0) {
                        return -1;
                    } else if (cleanO2.length() == 0) {
                        return 1;
                    } else {
                        return Integer.parseInt(cleanO1) - Integer.parseInt(cleanO2);
                    }
                } else if (o1.startsWith("*") && !o2.startsWith("*")) {
                    return -1;
                } else if (!o1.startsWith("*") && o2.startsWith("*")) {
                    return 1;
                } else {
                    return o2.length() - o1.length();
                }
            }
        });

        for (String labelItem : allTitles) {
            int index;
            if (labelItem.startsWith("*")) {
                if (labelItem.equals("*")) {
                    index = -2;
                } else {
                    String nextPosition = labelItem.substring(1);
                    int nextNbreakline = Integer.parseInt(nextPosition);
                    int breakingCursor = -1;
                    int lastPosition = 0;
                    for (int breakingCount = 0; breakingCount < nextNbreakline; breakingCount++) {
                        int currentPosition = doc.indexOf('\n', lastPosition + 1);
                        if (currentPosition != -1) {
                            lastPosition = currentPosition;
                            breakingCursor = lastPosition;
                        } else {
                            breakingCursor = lastPosition;
                            break;
                        }
                    }

                    index = breakingCursor;
                }
            } else {
                index = doc.indexOf(labelItem);
            }

            EmrLabel label = titleToLabel.get(labelItem);
            String labelName = label.getName();

            if (index == -2) {
                rangeMap.put(Range.closed(0, 0), labelName);
                result.addPart(labelName, labelItem, label);

                if (!Objects.equals(label.getType(), ValueType.exclude)) {
                    continue;
                }
            }

            while (index != -1) {
                String coveredTitle = rangeMap.get(index);
                if (coveredTitle != null) {
                    //已被更长的标签匹配到，应从下限后继续寻找
                    Map.Entry<Range<Integer>, String> coveredRange = rangeMap.getEntry(index);

                    int upperBound = coveredRange.getKey().upperEndpoint();
                    index = doc.indexOf(labelItem, upperBound + 1);

                } else {
                    break;
                }

            }

            if (index != -1) {
                rangeMap.put(Range.closed(index, index + labelItem.length()), labelName);

                result.addPart(labelName, labelItem, label);

                if (!Objects.equals(label.getType(), ValueType.exclude)) {
                    continue;
                }
            }
        }

//        for (Map.Entry<String, Label> entry : labelMap.entrySet()) {
//            String labelName = entry.getKey();
//            Label label = entry.getValue();
//
//            List<String> titles = entry.getValue().getOptionalTitles();
//            for (String labelItem : titles) {
//                int index;
//                if (labelItem.startsWith("*")) {
//                    if (labelItem.equals("*")) {
//                        index = -2;
//                    } else {
//                        String nextPosition = labelItem.substring(1);
//                        int nextNbreakline = Integer.parseInt(nextPosition);
//                        int breakingCursor = -1;
//                        int lastPosition = 0;
//                        for (int breakingCount = 0; breakingCount < nextNbreakline; breakingCount++) {
//                            int currentPosition = doc.indexOf('\n', lastPosition + 1);
//                            if (currentPosition != -1) {
//                                lastPosition = currentPosition;
//                                breakingCursor = lastPosition;
//                            } else {
//                                breakingCursor = lastPosition;
//                                break;
//                            }
//                        }
//
//                        index = breakingCursor;
//                    }
//                } else {
//                    index = doc.indexOf(labelItem);
//                }
//
//                if (index == -2) {
//                    rangeMap.put(Range.closed(0, 0), labelName);
//                    result.addPart(labelName, labelItem, label);
//
//                    if (!Objects.equals(label.getType(), ValueType.exclude)) {
//                        break;
//                    }
//                }
//
//                while (index != -1) {
//                    String coveredTitle = rangeMap.get(index);
//                    if (coveredTitle != null) {
//                        //已被更长的标签匹配到，应从下限后继续寻找
//                        Map.Entry<Range<Integer>, String> coveredRange = rangeMap.getEntry(index);
//
//                        int upperBound = coveredRange.getKey().upperEndpoint();
//                        index = doc.indexOf(labelItem, upperBound + 1);
//
//                    } else {
//                        break;
//                    }
//
//                }
//
//                if (index != -1) {
//                    rangeMap.put(Range.closed(index, index + labelItem.length()), labelName);
//
//                    result.addPart(labelName, labelItem, label);
//
//                    if (!Objects.equals(label.getType(), ValueType.exclude)) {
//                        break;
//                    }
//                }
//            }
//        }

        Map<Range<Integer>, String> indexMap = rangeMap.asMapOfRanges();
        String lastLabel = null;
        int lowerBound = -1;
        Map<String, String> labelValues = Maps.newHashMap();
        for (Map.Entry<Range<Integer>, String> entry : indexMap.entrySet()) {
            if (lowerBound == -1) {
                lowerBound = entry.getKey().upperEndpoint();
                lastLabel = entry.getValue();
                continue;
            }

            if (!Strings.isNullOrEmpty(lastLabel)) {
                String content = doc.substring(lowerBound, entry.getKey().lowerEndpoint());
                String value = content.trim();
                if (!Strings.isNullOrEmpty(value) &&
                        !Objects.equals(labelMap.get(lastLabel).getType(), ValueType.exclude)) {
                    labelValues.put(lastLabel, content.trim());
                }

                lastLabel = entry.getValue();
                lowerBound = entry.getKey().upperEndpoint();
            }
        }

        //proc last label
        if (!Strings.isNullOrEmpty(lastLabel)) {
            String value = doc.substring(lowerBound).trim();
            if (!Strings.isNullOrEmpty(value) &&
                    !Objects.equals(labelMap.get(lastLabel).getType(), ValueType.exclude)) {
                labelValues.put(lastLabel, value);
            }
        }

        extractValues(labelMap, labelValues, result);
    }

    private void extractValues(Map<String, EmrLabel> labelMap, Map<String, String> labelValues, JigsawResult result) {

        Map<String, String> extractMap = Maps.newHashMap(labelValues);
        for (Map.Entry<String, String> labelValue : extractMap.entrySet()) {
            EmrLabel label = labelMap.get(labelValue.getKey());
            ValueType type = label.getType();

            if (type == ValueType.complex || type == ValueType.combine) {
                parseDocumentParts(label.getSubLabels(), labelValue.getValue(), result);
            }

            label.getExtractor().extractValue(label, labelValue.getValue(), result);
        }
    }

    private String preProcessDocument(String doc) {
        String processed = ExtractorUtils.toDBC(doc);
        processed = ExtractorUtils.removeDuplicateBlank(processed);
        return processed;
    }
}
