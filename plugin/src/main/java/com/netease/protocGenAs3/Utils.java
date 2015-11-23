//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package com.netease.protocGenAs3;

public class Utils {
    public static String lowerCamelCase(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        appendLowerCamelCase(stringBuilder, s);
        return stringBuilder.toString();
    }

    public static String upperCamelCase(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        appendUpperCamelCase(stringBuilder, s);
        return stringBuilder.toString();
    }

    public static void appendLowerCamelCase(StringBuilder sb, String s) {
        sb.append(Character.toLowerCase(s.charAt(0)));
        appendCamelCase(sb, s);
    }

    public static void appendUpperCamelCase(StringBuilder sb, String s) {
        sb.append(Character.toUpperCase(s.charAt(0)));
        appendCamelCase(sb, s);
    }

    private static void appendCamelCase(StringBuilder sb, String s) {
        boolean upper = false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (upper) {
                if (Character.isLowerCase(c)) {
                    sb.append(Character.toUpperCase(c));
                    upper = false;
                    continue;
                } else {
                    sb.append('_');
                }
            }
            upper = c == '_';
            if (!upper) {
                sb.append(c);
            }
        }
    }
}
