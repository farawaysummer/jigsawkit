package com.fs.doc.jigsaw.trainer;

import com.fs.doc.jigsaw.EmrLabel;
import com.fs.doc.jigsaw.EmrTemplate;
import com.fs.doc.jigsaw.extractor.ExtractorUtils;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.javatuples.Pair;

import java.util.*;

public class EmrDocumentAnalyzer {
    private final CharMatcher separatorMatcher;

    public EmrDocumentAnalyzer() {
        this(new char[0]);
    }

    public EmrDocumentAnalyzer(char[] separators) {
        if (separators.length == 0) {
            separatorMatcher = CharMatcher.is(':');
        } else {
            separatorMatcher = CharMatcher.anyOf(new String(separators));
        }
    }

    public CharMatcher getSeparatorMatcher() {
        return separatorMatcher;
    }

    public Map<String, EmrLabel> analyze(String content, Map<String, EmrLabel> labelMap) {
        Set<String> titles = detectTitle(content, separatorMatcher);
        List<String> labelTitles = Lists.newArrayList(labelMap.keySet());
        Collections.sort(labelTitles, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.length() - o1.length();
            }
        });
        titles = adjustTitles(titles, labelTitles);
        return doLabelMapping(labelMap, titles, separatorMatcher);
    }

    public Set<String> adjustTitles(Set<String> titles, List<String> labelTitles) {
        Set<String> adjusted = Sets.newHashSet();
        boolean isAdjust = false;

        for (String title : titles) {
            String cleanTitle = separatorMatcher.trimTrailingFrom(title);
            String suffix = null;
            if (cleanTitle.length() < title.length()) {
                suffix = title.substring(cleanTitle.length());
            }

            for (String labelTitle : labelTitles) {
                if (Objects.equals(labelTitle, cleanTitle)) {
                    break;
                }

                if (cleanTitle.endsWith(labelTitle) && cleanTitle.length() > labelTitle.length()) {
                    adjusted.add(labelTitle + Strings.nullToEmpty(suffix));
                    isAdjust = true;
                    break;
                }
            }

            if (!isAdjust) {
                adjusted.add(title);
            }

            isAdjust = false;
        }

        return adjusted;
    }

    private Map<String, EmrLabel> doLabelMapping(Map<String, EmrLabel> labelMap, Set<String> titles, CharMatcher matcher) {
        Map<String, EmrLabel> mapping = Maps.newHashMap();
        Map<EmrLabel, Pair<String, Integer>> distances = Maps.newHashMap();
        for (String title : titles) {
            Pair<String, Integer> selected = selectLabel(labelMap.keySet(), EmrTemplate.normalizeTitle(title, matcher));
            if (selected == null) {
                continue;
            }

            EmrLabel label = labelMap.get(selected.getValue0());
            if (label != null) {
                if (distances.containsKey(label)) {
                    Pair<String, Integer> oldMatched = distances.get(label);
                    if (oldMatched.getValue1() > selected.getValue1()) {
                        mapping.remove(oldMatched.getValue0());
                        distances.put(label, Pair.with(title, selected.getValue1()));
                        mapping.put(title, label);
                    }
                } else {
                    distances.put(label, Pair.with(title, selected.getValue1()));
                    mapping.put(title, label);
                }
            }
        }

        return mapping;
    }

    private Pair<String, Integer> selectLabel(Set<String> labelTitles, String title) {
        int minDistance = -1;
        String minDesc = null;
        for (String desc : labelTitles) {
            if (desc.startsWith("*")) {
                continue;
            }

            int distance = LevenshteinDistance.getDefaultInstance().apply(title, desc);
            if (distance == 0) {
                return Pair.with(desc, 0);
            }

            if (minDistance == -1 || distance < minDistance) {
                minDistance = distance;
                minDesc = desc;
            }
        }

        if (minDistance > title.length() / 2) {
            return null;
        }

        return Pair.with(minDesc, minDistance);
    }

    public Set<String> detectTitle(String content, CharMatcher separatorMatcher) {
        String doc = ExtractorUtils.normalizeDocument(content);
        List<Integer> indexes = Lists.newArrayList();
        int startIndex = 0;

        int dotIndex = separatorMatcher.indexIn(doc, startIndex);
        while (dotIndex != -1) {
            if (indexes.size() > 0) {
                if (dotIndex == indexes.get(indexes.size() - 1) + 1) {
                    indexes.set(indexes.size() - 1, dotIndex);
                } else {
                    indexes.add(dotIndex);
                }
            } else {
                indexes.add(dotIndex);
            }

            startIndex = dotIndex + 1;
            dotIndex = separatorMatcher.indexIn(doc, startIndex);
        }

        Set<String> titles = Sets.newLinkedHashSet();
        for (int index = 0; index < indexes.size(); index++) {
            findPrev(doc, indexes, index, titles);
        }

        return titles;
    }

    private void findPrev(String doc, List<Integer> indexes, int dotIndex, Set<String> titles) {
        int index = indexes.get(dotIndex);
        int lowerBoundIndex;
        if (dotIndex == 0) {
            lowerBoundIndex = 0;
        } else {
            lowerBoundIndex = indexes.get(dotIndex - 1);
        }

        int upperBoundIndex = doc.length() - 1;
        if (dotIndex != indexes.size() - 1) {
            upperBoundIndex = indexes.get(dotIndex + 1);
        }

        int cursor = index;
        StringBuilder foundTitle = new StringBuilder();
        foundTitle.append(doc.charAt(index));
        boolean detectTitle = false;
        while (true) {
            cursor--;
            if (cursor < 0 ||
                    (cursor == lowerBoundIndex && lowerBoundIndex != 0)) break;

            char c = doc.charAt(cursor);
            if (CharMatcher.digit().matches(c)) {
//                foundTitle.setLength(0);
                break;
            }

            if (ExtractorUtils.isChineseByScript(c)) {
                detectTitle = true;
            }

            if (DeepTitleStopMatcher.INSTANCE.matches(c)) {
                if (!detectTitle) {
                    foundTitle.insert(0, c);
                    continue;
                }

                break;
            }

            foundTitle.insert(0, c);
        }

        int position = doc.indexOf(foundTitle.toString(), lowerBoundIndex) + foundTitle.length();
        while (true) {
            if (position > doc.length()) break;

            if (position >= upperBoundIndex) break;

            char c = doc.charAt(position);
            if (separatorMatcher.matches(c)) {
                foundTitle.append(c);
            } else {
                break;
            }

            position++;
        }

        String title = foundTitle.toString().trim();
        if (title.length() <= 12 && title.length() > 1) {
            titles.add(title);
        }
    }

    private static class TitleStopMatcher extends CharMatcher {
        static final TitleStopMatcher INSTANCE = new TitleStopMatcher();

        @Override
        public boolean matches(char c) {
            return CharMatcher.whitespace().matches(c);
        }

    }

    private static class DeepTitleStopMatcher extends CharMatcher {
        static final DeepTitleStopMatcher INSTANCE = new DeepTitleStopMatcher();

        @Override
        public boolean matches(char c) {

            return CharMatcher.whitespace().matches(c) || ExtractorUtils.isChinesePunctuation(c) || c == ';';
        }
    }
}
