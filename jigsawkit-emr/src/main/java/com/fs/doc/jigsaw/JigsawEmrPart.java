package com.fs.doc.jigsaw;

public class JigsawEmrPart {
    private String title;
    private String value;
    private EmrLabel label;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public EmrLabel getLabel() {
        return label;
    }

    public void setLabel(EmrLabel label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "JigsawPart{" +
                "title='" + title + '\'' +
                ", extractValue='" + value + '\'' +
                '}';
    }
}
