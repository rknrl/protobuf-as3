//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package com.netease.protocGenAs3;

import com.google.protobuf.DescriptorProtos;

import java.math.BigInteger;
import java.util.HashSet;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE;

public class MessageWriter {
    @SuppressWarnings("fallthrough")
    private static String getImportType(Scope<?> scope,
                                        DescriptorProtos.FieldDescriptorProto fdp) {
        switch (fdp.getType()) {
            case TYPE_ENUM:
//                if ( !fdp.hasDefaultValue() ) {
//                    return null;
//                }
                // fall-through
            case TYPE_MESSAGE:
                Scope<?> typeScope = scope.find(fdp.getTypeName());
                if (typeScope == null) {
                    throw new IllegalArgumentException(
                            fdp.getTypeName() + " not found.");
                }
                return typeScope.fullName;
            case TYPE_BYTES:
                return "ByteArray";
            default:
                return null;
        }
    }

    private static boolean isPrimitiveType(String as3type) {
        return !as3NullableType(as3type).equals(as3type);
    }

    private static String as3NullableType(String as3type) {
        if ("Number".equals(as3type)) {
            return "Num";
        }
        if ("int".equals(as3type)) {
            return "Int";
        }
        if ("uint".equals(as3type)) {
            return "UInt";
        }
        if ("Boolean".equals(as3type)) {
            return "Bool";
        }
        return as3type;
    }

    private static boolean isValueType(DescriptorProtos.FieldDescriptorProto.Type type) {
        switch (type) {
            case TYPE_DOUBLE:
            case TYPE_FLOAT:
            case TYPE_INT32:
            case TYPE_FIXED32:
            case TYPE_BOOL:
            case TYPE_UINT32:
            case TYPE_SFIXED32:
            case TYPE_SINT32:
            case TYPE_ENUM:
                return true;
            default:
                return false;
        }
    }

    private static String getActionScript3WireType(DescriptorProtos.FieldDescriptorProto.Type type) {
        switch (type) {
            case TYPE_DOUBLE:
            case TYPE_FIXED64:
            case TYPE_SFIXED64:
                return "FIXED_64_BIT";
            case TYPE_FLOAT:
            case TYPE_FIXED32:
            case TYPE_SFIXED32:
                return "FIXED_32_BIT";
            case TYPE_INT32:
            case TYPE_SINT32:
            case TYPE_UINT32:
            case TYPE_BOOL:
            case TYPE_INT64:
            case TYPE_UINT64:
            case TYPE_SINT64:
            case TYPE_ENUM:
                return "VARINT";
            case TYPE_STRING:
            case TYPE_MESSAGE:
            case TYPE_BYTES:
                return "LENGTH_DELIMITED";
            default:
                throw new IllegalArgumentException();
        }
    }

    private static String getAS3Type(Scope<?> scope,
                                     DescriptorProtos.FieldDescriptorProto fdp) {
        switch (fdp.getType()) {
            case TYPE_DOUBLE:
            case TYPE_FLOAT:
                return "Number";
            case TYPE_INT32:
            case TYPE_FIXED32:
            case TYPE_SFIXED32:
            case TYPE_SINT32:
                return "int";
            case TYPE_UINT32:
                return "uint";
            case TYPE_BOOL:
                return "Boolean";
            case TYPE_INT64:
            case TYPE_FIXED64:
            case TYPE_SFIXED64:
            case TYPE_SINT64:
                return "Int64";
            case TYPE_UINT64:
                return "UInt64";
            case TYPE_STRING:
                return "String";
            case TYPE_MESSAGE:
            case TYPE_ENUM:
                Scope<?> typeScope = scope.find(fdp.getTypeName());
                if (typeScope == null) {
                    throw new IllegalArgumentException(
                            fdp.getTypeName() + " not found.");
                }
                if (typeScope == scope) {
                    // workaround for mxmlc's bug.
                    return typeScope.fullName.substring(
                            typeScope.fullName.lastIndexOf('.') + 1);
                }
                return typeScope.fullName;
            case TYPE_BYTES:
                return "ByteArray";
            default:
                throw new IllegalArgumentException();
        }
    }

    private static void appendQuotedString(StringBuilder sb, String value) {
        sb.append('\"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\"':
                case '\\':
                    sb.append('\\');
                    sb.append(c);
                    break;
                default:
                    if (c >= 128 || Character.isISOControl(c)) {
                        sb.append("\\u");
                        sb.append(String.format("%04X", new Integer(c)));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('\"');
    }

    private static String defaultValue(Scope<?> scope, DescriptorProtos.FieldDescriptorProto fdp) {
        StringBuilder sb = new StringBuilder();
        String value = fdp.getDefaultValue();
        switch (fdp.getType()) {
            case TYPE_DOUBLE:
            case TYPE_FLOAT:
                if (value.equals("nan")) {
                    sb.append("NaN");
                } else if (value.equals("inf")) {
                    sb.append("Infinity");
                } else if (value.equals("-inf")) {
                    sb.append("-Infinity");
                } else {
                    sb.append(value);
                }
                break;
            case TYPE_UINT64: {
                long v = new BigInteger(value).longValue();
                sb.append("new UInt64(");
                sb.append(Long.toString(v & 0xFFFFFFFFL));
                sb.append(", ");
                sb.append(Long.toString((v >>> 32) & 0xFFFFFFFFL));
                sb.append(")");
            }
            break;
            case TYPE_INT64:
            case TYPE_FIXED64:
            case TYPE_SFIXED64:
            case TYPE_SINT64: {
                long v = Long.parseLong(value);
                sb.append("new Int64(");
                sb.append(Long.toString(v & 0xFFFFFFFFL));
                sb.append(", ");
                sb.append(Integer.toString((int) v >>> 32));
                sb.append(")");
            }
            break;
            case TYPE_INT32:
            case TYPE_FIXED32:
            case TYPE_SFIXED32:
            case TYPE_SINT32:
            case TYPE_UINT32:
            case TYPE_BOOL:
                sb.append(value);
                break;
            case TYPE_STRING:
                appendQuotedString(sb, value);
                break;
            case TYPE_ENUM:
                sb.append(scope.find(fdp.getTypeName()).
                        children.get(value).fullName);
                break;
            case TYPE_BYTES:
                sb.append("stringToByteArray(");
                sb.append("\"");
                sb.append(value);
                sb.append("\")");
                break;
            default:
                throw new IllegalArgumentException();
        }
        return sb.toString();
    }

    public static void writeMessage(Scope<DescriptorProtos.DescriptorProto> scope,
                                    StringBuilder content) {

        imports(scope, content);

        content.append("\tpublic final class " + scope.proto.getName() + " implements IMessage {\n"); // class
        writeConstructor(scope, content);
        fields(scope, content);
        write(scope, content);
        writeDelimitedTo(content);
        read(scope, content);
        parseDelimitedFrom(scope, content);
        writeToString(content);
        content.append("\t}\n");
    }

    private static void imports(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content) {
        content.append("\timport com.netease.protobuf.*;\n");
        content.append("\timport flash.utils.Endian;\n");
        content.append("\timport flash.utils.IDataInput;\n");
        content.append("\timport flash.utils.IDataOutput;\n");
        content.append("\timport flash.errors.IOError;\n");

        HashSet<String> importTypes = new HashSet<String>();

        for (DescriptorProtos.FieldDescriptorProto fdp : scope.proto.getFieldList()) {
            String importType = getImportType(scope, fdp);
            if (importType != null) {
                importTypes.add(importType);
            }
        }

        for (String importType : importTypes) {
            content.append("\timport " + importType + ";\n");
        }
    }

    private static String finalAS3Type(Scope<?> scope, DescriptorProtos.FieldDescriptorProto fdp) {
        String as3Type = getAS3Type(scope, fdp);
        switch (fdp.getLabel()) {
            case LABEL_OPTIONAL:
                return as3NullableType(as3Type);
            case LABEL_REQUIRED:
                return as3Type;
            case LABEL_REPEATED:
                return "Vector.<" + as3Type + ">";
            default:
                throw new IllegalArgumentException("Not supported");
        }
    }

    private static void writeConstructor(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content) {
        StringBuilder params = new StringBuilder();
        StringBuilder body = new StringBuilder();

        for (DescriptorProtos.FieldDescriptorProto fdp : scope.proto.getFieldList()) {
            String fieldName = Utils.lowerCamelCase(fdp.getName());
            String fieldType = finalAS3Type(scope, fdp);
            params.append(fieldName + ": " + fieldType + ",");
            body.append("\t\t\t_" + fieldName + " = " + fieldName + ";\n");
        }

        if (params.length() > 0) {
            params.delete(params.length() - 1, params.length()); // last comma
        }

        content.append("\t\tpublic function " + scope.proto.getName() + "(" + params.toString() + ") {\n");
        content.append(body);
        content.append("\t\t}\n\n");
    }

    private static void fields(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content) {
        for (DescriptorProtos.FieldDescriptorProto fdp : scope.proto.getFieldList()) {
            switch (fdp.getLabel()) {
                case LABEL_OPTIONAL:
                    optionalField(scope, content, fdp);
                    break;
                case LABEL_REQUIRED:
                    requiredField(scope, content, fdp);
                    break;
                case LABEL_REPEATED:
                    repeatedField(scope, content, fdp);
                    break;
                default:
                    throw new IllegalArgumentException("Not supported");
            }
        }
    }

    private static void optionalField(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        String fieldName = Utils.lowerCamelCase(fdp.getName());
        String privateFieldName = "_" + fieldName;
        String upperFieldName = Utils.upperCamelCase(fdp.getName());
        String as3Type = getAS3Type(scope, fdp);
        String fieldType = as3NullableType(as3Type);

        content.append("\t\tprivate var " + privateFieldName + ": " + fieldType + ";\n\n");

        content.append("\t\tpublic function get has" + upperFieldName + "():Boolean {\n");
        content.append("\t\t\treturn " + privateFieldName + " != null;\n");
        content.append("\t\t}\n\n");

        if (isPrimitiveType(as3Type)) {
            content.append("\t\tpublic function get" + upperFieldName + "():" + fieldType + " {\n");
            content.append("\t\t\treturn " + privateFieldName + ";\n");
            content.append("\t\t}\n\n");
        }

        content.append("\t\tpublic function get " + fieldName + "():" + as3Type + " {\n");
        if (fdp.hasDefaultValue()) {
            content.append("\t\t\tif(!has" + upperFieldName + ") {\n");
            content.append("\t\t\t\treturn " + defaultValue(scope, fdp) + ";\n");
            content.append("\t\t\t}\n");
        }
        if (isPrimitiveType(as3Type)) {
            content.append("\t\t\treturn " + privateFieldName + ".value;\n");
        } else {
            content.append("\t\t\treturn " + privateFieldName + ";\n");
        }
        content.append("\t\t}\n\n");
    }

    private static void requiredField(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        String fieldName = Utils.lowerCamelCase(fdp.getName());
        String privateFieldName = "_" + fieldName;
        String fieldType = getAS3Type(scope, fdp);

        content.append("\t\tprivate var " + privateFieldName + ":" + fieldType);
        if (fdp.hasDefaultValue()) {
            content.append(" = " + defaultValue(scope, fdp));
        }
        content.append(";\n\n");

        content.append("\t\tpublic function get " + fieldName + "(): " + fieldType + "{\n");
        content.append("\t\t return " + privateFieldName + ";\n");
        content.append("\t\t}\n\n");
    }

    private static void repeatedField(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        String fieldName = Utils.lowerCamelCase(fdp.getName());
        String privateFieldName = "_" + fieldName;
        String type = getAS3Type(scope, fdp);
        String fieldType = "Vector.<" + type + ">";
        content.append("\t\tprivate var " + privateFieldName + ":" + fieldType + ";\n\n");

        content.append("\t\tpublic function get " + fieldName + "(): " + fieldType + "{\n");
        content.append("\t\t return " + privateFieldName + ";\n");
        content.append("\t\t}\n\n");
    }

    private static void write(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content) {
        content.append("\t\tpublic final function writeToBuffer(output:WritingBuffer):void {\n");
        for (DescriptorProtos.FieldDescriptorProto fdp : scope.proto.getFieldList()) {
            switch (fdp.getLabel()) {
                case LABEL_OPTIONAL:
                    writeOptional(scope, content, fdp);
                    break;
                case LABEL_REQUIRED:
                    writeRequired(content, fdp);
                    break;
                case LABEL_REPEATED:
                    writeRepeated(content, fdp);
                    break;
            }
        }
    }

    private static void writeRepeated(StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        String fieldName = Utils.lowerCamelCase(fdp.getName());

        if (fdp.hasOptions() && fdp.getOptions().getPacked()) {
            content.append("" +
                    "\t\t\tif (" + fieldName + " != null && " + fieldName + ".length > 0) {\n" +
                    "\t\t\t\tWriteUtils.writeTag(output, WireType.LENGTH_DELIMITED, " + Integer.toString(fdp.getNumber()) + ");\n" +
                    "\t\t\t\tWriteUtils.writePackedRepeated(output, WriteUtils.write$" + fdp.getType().name() + ", " + fieldName + ");\n" +
                    "\t\t\t}\n");
        } else {
            String indexName = fieldName + "Index";
            content.append("" +
                    "\t\t\tfor (var " + indexName + ":uint = 0; " + indexName + " < " + fieldName + ".length; ++" + indexName + ") {\n" +
                    "\t\t\t\tWriteUtils.writeTag(output, WireType." + getActionScript3WireType(fdp.getType()) + ", " + Integer.toString(fdp.getNumber()) + ");\n" +
                    "\t\t\t\tWriteUtils.write$" + fdp.getType().name() + "(output, " + fieldName + "[" + indexName + "]);\n" +
                    "\t\t\t}\n"
            );
        }
    }

    private static void writeRequired(StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        content.append("" +
                "\t\t\tWriteUtils.writeTag(output, WireType." + getActionScript3WireType(fdp.getType()) + ", " + Integer.toString(fdp.getNumber()) + ");\n" +
                "\t\t\tWriteUtils.write$" + fdp.getType().name() + "(output, " + Utils.lowerCamelCase(fdp.getName()) + ");\n"
        );
    }

    private static void writeOptional(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        String as3Type = getAS3Type(scope, fdp);

        content.append("\t\t\tif (has" + Utils.upperCamelCase(fdp.getName()) + ") {\n");
        content.append("\t\t\t\tWriteUtils.writeTag(output, WireType." + getActionScript3WireType(fdp.getType()) + ", " + Integer.toString(fdp.getNumber()) + ");\n");
        if (isPrimitiveType(as3Type)) {
            content.append("\t\t\t\tWriteUtils.write$" + fdp.getType().name() + "(output, _" + fdp.getName() + ".value);\n");
        } else {
            content.append("\t\t\t\tWriteUtils.write$" + fdp.getType().name() + "(output, _" + fdp.getName() + ");\n");
        }
        content.append("\t\t\t}\n");
    }

    private static void writeDelimitedTo(StringBuilder content) {
        content.append("\t\t}\n\n");
        content.append("\t\tpublic final function writeDelimitedTo(output:IDataOutput):void {\n");
        content.append("\t\t\tconst buffer:WritingBuffer = new WritingBuffer();\n");
        content.append("\t\t\tWriteUtils.write$TYPE_MESSAGE(buffer, this);\n");
        content.append("\t\t\tbuffer.toNormal(output);\n");
        content.append("\t\t}\n\n");
    }

    private static void read(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content) {
        content.append("\t\tpublic static function readFromSlice(input:IDataInput, bytesAfterSlice:uint):" + scope.proto.getName() + " {\n");
        readInitVars(scope, content);
        content.append("\t\t\twhile (input.bytesAvailable > bytesAfterSlice) {\n");
        content.append("\t\t\t\tvar tag:uint = ReadUtils.read$TYPE_UINT32(input);\n");
        content.append("\t\t\t\tswitch (tag >>> 3) {\n");

        for (DescriptorProtos.FieldDescriptorProto fdp : scope.proto.getFieldList()) {
            content.append("\t\t\t\tcase " + Integer.toString(fdp.getNumber()) + ":\n");
            switch (fdp.getLabel()) {
                case LABEL_OPTIONAL:
                case LABEL_REQUIRED:
                    readField(scope, content, fdp);
                    break;
                case LABEL_REPEATED:
                    readRepeated(scope, content, fdp);
                    break;
            }
            content.append("\t\t\t\t\tbreak;\n");
        }

        content.append("\t\t\t\tdefault:\n");
        content.append("\t\t\t\t\tReadUtils.skip(input, tag & 7);\n");
        content.append("\t\t\t\t}\n");
        content.append("\t\t\t}\n");
        readRequiredChecks(scope, content);
        readReturn(scope, content);
        content.append("\t\t}\n\n");
    }

    private static void readInitVars(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content) {
        for (DescriptorProtos.FieldDescriptorProto fdp : scope.proto.getFieldList()) {
            String fieldName = Utils.lowerCamelCase(fdp.getName());
            String fieldType = finalAS3Type(scope, fdp);
            switch (fdp.getLabel()) {
                case LABEL_OPTIONAL:
                case LABEL_REQUIRED:
                    content.append("\t\t\tvar " + fieldName + ":" + fieldType + ";\n");
                    content.append("\t\t\tvar " + fdp.getName() + "$count:uint = 0;\n");
                    break;
                case LABEL_REPEATED:
                    content.append("\t\t\tvar " + fieldName + ":" + fieldType + " = new " + fieldType + ";\n");
                    break;
            }
        }
    }

    private static void readReturn(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content) {
        content.append("\t\t\treturn new " + scope.proto.getName() + "(");

        for (int i = 0; i < scope.proto.getFieldCount(); i++) {
            DescriptorProtos.FieldDescriptorProto fdp = scope.proto.getField(i);
            String fieldName = Utils.lowerCamelCase(fdp.getName());
            content.append(fieldName);
            if (i < scope.proto.getFieldCount() - 1) {
                content.append(',');
            }
        }
        content.append(")\n");
    }

    private static void readField(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        content.append("\t\t\t\t\tif (" + fdp.getName() + "$count != 0) {\n");
        content.append("\t\t\t\t\t\tthrow new IOError('Bad data format: " + scope.proto.getName() + '.' + Utils.lowerCamelCase(fdp.getName()) + " cannot be set twice.');\n");
        content.append("\t\t\t\t\t}\n");
        content.append("\t\t\t\t\t++" + fdp.getName() + "$count;\n");
        if (fdp.getType() == TYPE_MESSAGE) {
            content.append("\t\t\t\t\tvar bytesAfterSlice: uint = ReadUtils.readBytesAfterSlice(input);\n");
            content.append("\t\t\t\t\t" + Utils.lowerCamelCase(fdp.getName()) + " =  " + getAS3Type(scope, fdp) + ".readFromSlice(input, bytesAfterSlice);\n");
            content.append("\t\t\t\t\tif (input.bytesAvailable != bytesAfterSlice) {\n");
            content.append("\t\t\t\t\t\tthrow new IOError('Invalid nested message');\n");
            content.append("\t\t\t\t\t}\n");

        } else if (fdp.getType() == TYPE_ENUM) {
            content.append("\t\t\t\t\t" + Utils.lowerCamelCase(fdp.getName()) + " = " + getAS3Type(scope, fdp) + ".valuesById[ReadUtils.read$TYPE_ENUM(input)];\n");
        } else {
            String as3Type = getAS3Type(scope, fdp);
            content.append("\t\t\t\t\t");
            if (fdp.getLabel() == LABEL_OPTIONAL && isPrimitiveType(as3Type)) {
                String fieldType = as3NullableType(as3Type);
                content.append(Utils.lowerCamelCase(fdp.getName()) + " = new " + fieldType + "(ReadUtils.read$" + fdp.getType().name() + "(input));\n");
            } else {
                content.append(Utils.lowerCamelCase(fdp.getName()) + " = ReadUtils.read$" + fdp.getType().name() + "(input);\n");
            }
        }
    }

    private static void readRepeated(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        switch (fdp.getType()) {
            case TYPE_DOUBLE:
            case TYPE_FLOAT:
            case TYPE_BOOL:
            case TYPE_INT32:
            case TYPE_FIXED32:
            case TYPE_UINT32:
            case TYPE_SFIXED32:
            case TYPE_SINT32:
            case TYPE_INT64:
            case TYPE_FIXED64:
            case TYPE_UINT64:
            case TYPE_SFIXED64:
            case TYPE_SINT64:
            case TYPE_ENUM:
                readRepeatedBasic(content, fdp);
        }
        if (fdp.getType() == TYPE_MESSAGE) {
            readRepeatedMessage(scope, content, fdp);
        } else if (fdp.getType() == TYPE_ENUM) {
            readRepeatedEnum(scope, content, fdp);
        } else {
            content.append("\t\t\t\t\t" + Utils.lowerCamelCase(fdp.getName() + ".push(ReadUtils.read$" + fdp.getType().name()) + "(input));");
        }
    }

    private static void readRepeatedBasic(StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        content.append("" +
                "\t\t\t\t\tif ((tag & 7) == WireType.LENGTH_DELIMITED) {\n" +
                "\t\t\t\t\t\tReadUtils.readPackedRepeated(input, ReadUtils.read$" + fdp.getType().name() + ", Vector.<Object>(" + Utils.lowerCamelCase(fdp.getName()) + "));\n" +
                "\t\t\t\t\t\tbreak;\n" +
                "\t\t\t\t\t}\n"
        );
    }

    private static void readRepeatedMessage(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        content.append("" +
                "\t\t\t\t\tvar bytesAfterSlice: uint = ReadUtils.readBytesAfterSlice(input);\n" +
                "\t\t\t\t\tconst " + fdp.getName() + "$element:" + getAS3Type(scope, fdp) + " = " + getAS3Type(scope, fdp) + ".readFromSlice(input, bytesAfterSlice);\n" +
                "\t\t\t\t\t" + Utils.lowerCamelCase(fdp.getName()) + ".push(" + fdp.getName() + "$element);\n" +
                "\t\t\t\t\tif (input.bytesAvailable != bytesAfterSlice) {\n" +
                "\t\t\t\t\t\tthrow new IOError('Invalid nested message');\n" +
                "\t\t\t\t\t}\n"
        );
    }

    private static void readRepeatedEnum(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content, DescriptorProtos.FieldDescriptorProto fdp) {
        content.append("\t\t\t\t\t" + Utils.lowerCamelCase(fdp.getName()) + ".push(" + getAS3Type(scope, fdp) + ".valuesById[ReadUtils.read$" + fdp.getType().name() + "(input)]);");
    }

    private static void readRequiredChecks(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content) {
        for (DescriptorProtos.FieldDescriptorProto fdp : scope.proto.getFieldList()) {
            if (fdp.getLabel() == LABEL_REQUIRED) {
                content.append("\t\t\tif (" + fdp.getName() + "$count != 1) {\n");
                content.append("\t\t\t\tthrow new IOError('Bad data format: " + scope.proto.getName() + '.' + Utils.lowerCamelCase(fdp.getName()) + " must be set');\n");
                content.append("\t\t\t}\n");
            }
        }
    }

    private static void parseDelimitedFrom(Scope<DescriptorProtos.DescriptorProto> scope, StringBuilder content) {
        content.append("\t\tpublic static function parseDelimitedFrom(input:IDataInput):" + scope.proto.getName() + "{\n");
        content.append("\t\t\tinput.endian = Endian.LITTLE_ENDIAN;\n");
        content.append("\t\t\tvar bytesAfterSlice: uint = ReadUtils.readBytesAfterSlice(input);\n");
        content.append("\t\t\tconst message: " + scope.proto.getName() + " = " + scope.proto.getName() + ".readFromSlice(input, bytesAfterSlice);\n");
        content.append("\t\t\tif (input.bytesAvailable != bytesAfterSlice) {\n");
        content.append("\t\t\t\tthrow new IOError('Invalid nested message');\n");
        content.append("\t\t\t}\n");
        content.append("\t\t\treturn message;\n");
        content.append("\t\t}\n\n");
    }

    private static void writeToString(StringBuilder content) {
        content.append("\t\tpublic function toString():String {\n");
        content.append("\t\t	return messageToString(this);\n");
        content.append("\t\t}\n\n");
    }
}
