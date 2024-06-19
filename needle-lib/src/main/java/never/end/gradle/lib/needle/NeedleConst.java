package never.end.gradle.lib.needle;


import java.io.File;

public class NeedleConst {

    public static final String PACKAGE = "never.end.needle";

    private static final String MappingName = "needle.mapping";
    private static final String MappingFilePath = ".generated.ap_generated_sources.variant.out."+PACKAGE;

    public static String getPackagePath() {
        return PACKAGE.replace(".", File.separator);
    }

    public static String getMappingFile(String buildPath, String variant) {
        return buildPath + MappingFilePath.replace(".", File.separator).replace("variant", variant)
                + File.separator + MappingName;
    }
}
