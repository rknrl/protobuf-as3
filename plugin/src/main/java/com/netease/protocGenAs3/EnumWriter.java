//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package com.netease.protocGenAs3;

import com.google.protobuf.DescriptorProtos;

public class EnumWriter {
    public static void writeEnum(Scope<DescriptorProtos.EnumDescriptorProto> scope,
                                 StringBuilder content) {
        content.append("\timport com.netease.protobuf.Enum;\n");
        content.append("\tpublic final class ");
        content.append(scope.proto.getName());
        content.append(" implements Enum {\n");
        for (DescriptorProtos.EnumValueDescriptorProto evdp : scope.proto.getValueList()) {
            content.append("\t\tpublic static const ");
            content.append(evdp.getName());
            content.append(":").append(scope.proto.getName());
            content.append(" = new ").append(scope.proto.getName());
            content.append("(");
            content.append(evdp.getNumber()).append(", \"");
            content.append(evdp.getName());
            content.append("\")");
            content.append(";\n");
        }
        content.append("\t\tprivate var _id:int;\n");
        content.append("\t\tprivate var _name:String;\n");
        content.append("\t\tpublic function ").append(scope.proto.getName()).append("(id:int, name:String) {\n");
        content.append("\t\t\tthis._id=id\n");
        content.append("\t\t\tthis._name=name\n");
        content.append("\t\t}\n\n");
        content.append("\t\tpublic function id():int {\n");
        content.append("\t\t\treturn _id;\n");
        content.append("\t\t}\n");
        content.append("\t\tpublic function name():String {\n");
        content.append("\t\t\treturn _name;\n");
        content.append("\t\t}\n");
        {
            content.append("\t\tpublic static const values : Vector.<")
                    .append(scope.proto.getName())
                    .append("> = Vector.<")
                    .append(scope.proto.getName()).append(">([");
            boolean first = true;
            for (DescriptorProtos.EnumValueDescriptorProto evdp : scope.proto.getValueList()) {
                if (!first) {
                    content.append(",");
                } else {
                    first = false;
                }
                content.append("\n");
                content.append("\t\t\t");
                content.append(evdp.getName());
            }
            content.append("\n\t\t]);\n");
        }
        {
            content.append("\t\tpublic static const valuesById : Object = {};");
            for (DescriptorProtos.EnumValueDescriptorProto evdp : scope.proto.getValueList()) {
                content.append("\n");
                content.append("\t\t");
                content.append("valuesById[");
                content.append(evdp.getNumber());
                content.append("]");
                content.append(" = ");
                content.append(evdp.getName());
                content.append(";");
            }
        }
        content.append("\t}\n");
    }
}
