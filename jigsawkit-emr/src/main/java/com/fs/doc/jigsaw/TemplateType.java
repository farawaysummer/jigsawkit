package com.fs.doc.jigsaw;

import com.google.common.collect.Maps;

import java.util.Map;

public enum TemplateType {
    HET_PATIENT("病历患者信息"),
    HET_BASIC("基本健康信息"),
    HET_EVENT("卫生事件摘要"),
    HET_FEE("医疗费用信息"),
    CLI_RECORD("门急诊病历信息"),
    CLI_EMER_RECORD("急诊留观病历信息"),
    INP_IN_RECORD("入院记录"),
    INP_INOUT_RECORD("24H内入出院记录"),
    INP_INHOS_DEATH("24H内入院死亡记录"),
    INP_FIRST_COURSE("首次病程记录"),
    INP_DAILY_COURSE("日常病程记录"),
    INP_SUPERIOR_COURSE("上级医师查房记录"),
    INP_INTR_RECORD("疑难病例讨论记录"),
    INP_SHIFT_RECORD("交接班记录"),
    INP_CHANGE_DEPT("住院转科记录"),
    INP_STAGE_COURSE("阶段小结记录"),
    INP_RESCUE("抢救记录"),
    INP_CONSULTATION("会诊记录"),
    INP_PREOPS_SUMMARY("术前小结记录"),
    INP_PREOPS_DISCUSS("术前讨论记录"),
    INP_OPSAFT_RECORD("术后首次病程记录"),
    INP_OUT_RECORD("出院记录"),
    INP_DEATH_RECORD("死亡记录"),
    INP_DEATH_DISCUSS("死亡病例讨论记录"),
    INP_OUT_SUMMARY("出院小结"),
    INP_OUT_TRANS("转院记录"),
    TRT_RECORD("治疗记录"),
    TRT_OPS_RECORD("一般手术记录"),
    TRT_ANESBEF_RECORD("麻醉术前访视记录"),
    TRT_ANES_RECORD("麻醉记录"),
    TRT_ANESAFT_RECORD("麻醉术后访视记录"),
    TRT_BLOOD_RECORD("输血记录"),
    INF_OPS("手术知情同意书"),
    INF_ANES("麻醉知情同意书"),
    INF_BLOOD("输血治疗同意书"),
    INF_TREAT("特殊检查及特殊治疗同意书"),
    INF_CRIT("病危(重)通知书"),
    INF_OTHER("其他知情同意书"),
    NRS_RECORD("一般护理记录"),
    NRS_CRIT_RECORD("病危（重）护理记录"),
    NRS_OPS_RECORD("手术护理记录"),
    NRS_SIGN_RECORD("生命体征测量记录"),
    NRS_INOUT_RECORD("患者出入量记录"),
    NRS_HIGHMET_USE("高值耗材使用记录"),
    NRS_INHOS_RECORD("入院评估记录"),
    NRS_PLAN_RECORD("护理计划记录"),
    NRS_OUTHOS_RECORD("出院评估与指导记录"),
    BIRTH_DELIVERY_RECORD("待产记录"),
    BIRTH_RECORD("自然分娩记录"),
    BIRTH_CESAREAN_RECORD("剖宫产手术记录");

    private final String templateName;

    TemplateType(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public static Map<String, String> getNameMapping() {
        Map<String, String> mapping = Maps.newHashMap();

        for (TemplateType type : TemplateType.values()) {
            mapping.put(type.getTemplateName(), type.name());
        }

        return mapping;
    }

    public static String[] getTemplateNames() {
        int size = TemplateType.values().length;
        String[] templateNames = new String[size];

        for (int index = 0; index < size; index++) {
            templateNames[index] = TemplateType.values()[index].templateName;
        }

        return templateNames;
    }
}
