package ru.runa.wfe.web.template;

public enum FormComponentTemplate {
    BigDecimalFormat("<input type=\"text\" name=\"%s\" class=\"inputNumber\" value=\"33\" />"),
    BooleanFormat("<input type=\"checkbox\" name=\"%s\" class=\"inputBoolean\" checked=\"checked\" />"),
    DateFormat("<input type=\"text\" name=\"%s\" class=\"inputDate\" value=\"01.01.2000\" />"),
    DateTimeFormat("<input type=\"text\" name=\"%s\" class=\"inputDateTime\" value=\"01.01.2000 00:00\" />"),
    DoubleFormat("<input type=\"text\" name=\"%s\" class=\"inputNumber\" value=\"33\" />"),
    ExecutorFormat("<select name=\"%s\"><option value=\"\"> ------------------------- </option><option value=\"ID303\">Ф.И.О 1</option></select>\n"),
    FileFormat("<input type=\"file\" name=\"%s\" class=\"inputFile\" />"),
    //FormattedTextFormat(""),
    GroupFormat("<select name=\"%s\"><option value=\"\"> ------------------------- </option><option value=\"ID303\">Ф.И.О 1</option></select>\n"),
    //ListFormat(""),
    LongFormat("<input type=\"text\" name=\"%s\" class=\"inputNumber\" value=\"33\" />"),
    //MapFormat(""),
    //ProcessIdFormat(""),
    StringFormat("<input type=\"text\" name=\"%s\" class=\"inputString\" value=\"\" />"),
    TextFormat("<textarea name=\"%s\" class=\"inputText\"></textarea>"),
    TimeFormat("<input type=\"text\" name=\"%s\" class=\"inputTime\" value=\"00:00\" />"),
    //UserTypeFormat(""),
    ActorFormat("<select name=\"%s\"><option value=\"\"> ------------------------- </option><option value=\"ID303\">Ф.И.О 1</option></select>\n");

    private final String value;

    FormComponentTemplate(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
