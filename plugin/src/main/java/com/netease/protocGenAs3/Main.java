// vim: fileencoding=utf-8 tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protocGenAs3;

import com.google.protobuf.ExtensionRegistry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.google.protobuf.DescriptorProtos.*;
import static google.protobuf.compiler.Plugin.CodeGeneratorRequest;
import static google.protobuf.compiler.Plugin.CodeGeneratorResponse;

public final class Main {

    private static final String[] ACTIONSCRIPT_KEYWORDS = {
            "as", "break", "case", "catch", "class", "const", "continue", "default",
            "delete", "do", "else", "extends", "false", "finally", "for",
            "function", "if", "implements", "import", "in", "instanceof",
            "interface", "internal", "is", "native", "new", "null", "package",
            "private", "protected", "public", "return", "super", "switch", "this",
            "throw", "to", "true", "try", "typeof", "use", "var", "void", "while",
            "with"
    };

    private static void addServiceToScope(Scope<?> scope,
                                          ServiceDescriptorProto sdp, boolean export) {
        scope.addChild( sdp.getName(), sdp, export );
    }

    private static void addExtensionToScope(Scope<?> scope,
                                            FieldDescriptorProto efdp, boolean export) {
        StringBuilder sb = new StringBuilder();
        appendLowerCamelCase( sb, efdp.getName() );
        scope.addChild( sb.toString(), efdp, export );
    }

    private static void addEnumToScope(Scope<?> scope, EnumDescriptorProto edp,
                                       boolean export) {
        assert (edp.hasName());
        Scope<EnumDescriptorProto> enumScope =
                scope.addChild( edp.getName(), edp, export );
        for ( EnumValueDescriptorProto evdp : edp.getValueList() ) {
            Scope<EnumValueDescriptorProto> enumValueScope =
                    enumScope.addChild( evdp.getName(), evdp, false );
            scope.addChild( evdp.getName(), enumValueScope, false );
        }
    }

    private static void addMessageToScope(Scope<?> scope, DescriptorProto dp,
                                          boolean export) {
        Scope<DescriptorProto> messageScope =
                scope.addChild( dp.getName(), dp, export );
        for ( EnumDescriptorProto edp : dp.getEnumTypeList() ) {
            addEnumToScope( messageScope, edp, export );
        }
        for ( DescriptorProto nested : dp.getNestedTypeList() ) {
            addMessageToScope( messageScope, nested, export );
        }
    }

    private static Scope<Object> buildScopeTree(CodeGeneratorRequest request) {
        Scope<Object> root = Scope.root();
        List<String> filesToGenerate = request.getFileToGenerateList();
        for ( FileDescriptorProto fdp : request.getProtoFileList() ) {
            Scope<?> packageScope = fdp.hasPackage() ?
                    root.findOrCreate( fdp.getPackage() ) : root;
            boolean export = filesToGenerate.contains( fdp.getName() );
            for ( ServiceDescriptorProto sdp : fdp.getServiceList() ) {
                addServiceToScope( packageScope, sdp, export );
            }
            for ( FieldDescriptorProto efdp : fdp.getExtensionList() ) {
                addExtensionToScope( packageScope, efdp, export );
            }
            for ( EnumDescriptorProto edp : fdp.getEnumTypeList() ) {
                addEnumToScope( packageScope, edp, export );
            }
            for ( DescriptorProto dp : fdp.getMessageTypeList() ) {
                addMessageToScope( packageScope, dp, export );
            }
        }
        return root;
    }

    @SuppressWarnings( "fallthrough" )
    private static String getImportType(Scope<?> scope,
                                        FieldDescriptorProto fdp) {
        switch( fdp.getType() ) {
            case TYPE_ENUM:
//                if ( !fdp.hasDefaultValue() ) {
//                    return null;
//                }
                // fall-through
            case TYPE_MESSAGE:
                Scope<?> typeScope = scope.find( fdp.getTypeName() );
                if ( typeScope == null ) {
                    throw new IllegalArgumentException(
                            fdp.getTypeName() + " not found." );
                }
                return typeScope.fullName;
            case TYPE_BYTES:
                return "flash.utils.ByteArray";
            default:
                return null;
        }
    }

    private static boolean isValueType(FieldDescriptorProto.Type type) {
        switch( type ) {
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

    private static String getActionScript3WireType(
            FieldDescriptorProto.Type type) {
        switch( type ) {
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

    private static String getActionScript3Type(Scope<?> scope,
                                               FieldDescriptorProto fdp) {
        switch( fdp.getType() ) {
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
                Scope<?> typeScope = scope.find( fdp.getTypeName() );
                if ( typeScope == null ) {
                    throw new IllegalArgumentException(
                            fdp.getTypeName() + " not found." );
                }
                if ( typeScope == scope ) {
                    // workaround for mxmlc's bug.
                    return typeScope.fullName.substring(
                            typeScope.fullName.lastIndexOf( '.' ) + 1 );
                }
                return typeScope.fullName;
            case TYPE_BYTES:
                return "flash.utils.ByteArray";
            default:
                throw new IllegalArgumentException();
        }
    }

    private static void appendWriteFunction(StringBuilder content,
                                            Scope<?> scope, FieldDescriptorProto fdp) {
        switch( fdp.getLabel() ) {
            case LABEL_REQUIRED:
                throw new IllegalArgumentException();
            case LABEL_OPTIONAL:
                content.append( "com.netease.protobuf.Extension.writeFunction(com.netease.protobuf.WireType." );
                content.append( getActionScript3WireType( fdp.getType() ) );
                content.append( ", " );
                break;
            case LABEL_REPEATED:
                if ( fdp.hasOptions() && fdp.getOptions().getPacked() ) {
                    content.append( "com.netease.protobuf.Extension.packedRepeatedWriteFunction(" );
                } else {
                    content.append( "com.netease.protobuf.Extension.repeatedWriteFunction(com.netease.protobuf.WireType." );
                    content.append( getActionScript3WireType( fdp.getType() ) );
                    content.append( ", " );
                }
                break;
        }
        content.append( "com.netease.protobuf.WriteUtils.write$" );
        content.append( fdp.getType().name() );
        content.append( ")" );
    }

    private static void appendReadFunction(StringBuilder content,
                                           Scope<?> scope, FieldDescriptorProto fdp) {
        if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE ) {
            switch( fdp.getLabel() ) {
                case LABEL_REQUIRED:
                    throw new IllegalArgumentException();
                case LABEL_OPTIONAL:
                    content.append( "com.netease.protobuf.Extension.messageReadFunction(" );
                    break;
                case LABEL_REPEATED:
                    assert (!(fdp.hasOptions() && fdp.getOptions().getPacked()));
                    content.append( "com.netease.protobuf.Extension.repeatedMessageReadFunction(" );
                    break;
            }
            content.append( getActionScript3Type( scope, fdp ) );
            content.append( ")" );
        } else {
            switch( fdp.getLabel() ) {
                case LABEL_REQUIRED:
                    throw new IllegalArgumentException();
                case LABEL_OPTIONAL:
                    content.append( "com.netease.protobuf.Extension.readFunction(" );
                    break;
                case LABEL_REPEATED:
                    content.append( "com.netease.protobuf.Extension.repeatedReadFunction(" );
                    break;
            }
            content.append( "com.netease.protobuf.ReadUtils.read$" );
            content.append( fdp.getType().name() );
            content.append( ")" );
        }
    }

    private static void appendQuotedString(StringBuilder sb, String value) {
        sb.append( '\"' );
        for ( int i = 0; i < value.length(); i++ ) {
            char c = value.charAt( i );
            switch( c ) {
                case '\"':
                case '\\':
                    sb.append( '\\' );
                    sb.append( c );
                    break;
                default:
                    if ( c >= 128 || Character.isISOControl( c ) ) {
                        sb.append( "\\u" );
                        sb.append( String.format( "%04X", new Integer( c ) ) );
                    } else {
                        sb.append( c );
                    }
            }
        }
        sb.append( '\"' );
    }

    private static void appendDefaultValue(StringBuilder sb, Scope<?> scope,
                                           FieldDescriptorProto fdp) {
        String value = fdp.getDefaultValue();
        switch( fdp.getType() ) {
            case TYPE_DOUBLE:
            case TYPE_FLOAT:
                if ( value.equals( "nan" ) ) {
                    sb.append( "NaN" );
                } else if ( value.equals( "inf" ) ) {
                    sb.append( "Infinity" );
                } else if ( value.equals( "-inf" ) ) {
                    sb.append( "-Infinity" );
                } else {
                    sb.append( value );
                }
                break;
            case TYPE_UINT64: {
                long v = new BigInteger( value ).longValue();
                sb.append( "new UInt64(" );
                sb.append( Long.toString( v & 0xFFFFFFFFL ) );
                sb.append( ", " );
                sb.append( Long.toString( (v >>> 32) & 0xFFFFFFFFL ) );
                sb.append( ")" );
            }
            break;
            case TYPE_INT64:
            case TYPE_FIXED64:
            case TYPE_SFIXED64:
            case TYPE_SINT64: {
                long v = Long.parseLong( value );
                sb.append( "new Int64(" );
                sb.append( Long.toString( v & 0xFFFFFFFFL ) );
                sb.append( ", " );
                sb.append( Integer.toString( (int) v >>> 32 ) );
                sb.append( ")" );
            }
            break;
            case TYPE_INT32:
            case TYPE_FIXED32:
            case TYPE_SFIXED32:
            case TYPE_SINT32:
            case TYPE_UINT32:
            case TYPE_BOOL:
                sb.append( value );
                break;
            case TYPE_STRING:
                appendQuotedString( sb, value );
                break;
            case TYPE_ENUM:
                sb.append( scope.find( fdp.getTypeName() ).
                        children.get( value ).fullName );
                break;
            case TYPE_BYTES:
                sb.append( "stringToByteArray(" );
                sb.append( "\"" );
                sb.append( value );
                sb.append( "\")" );
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static void appendLowerCamelCase(StringBuilder sb, String s) {
        if ( Arrays.binarySearch( ACTIONSCRIPT_KEYWORDS, s ) >= 0 ) {
            sb.append( "__" );
        }
        sb.append( Character.toLowerCase( s.charAt( 0 ) ) );
        boolean upper = false;
        for ( int i = 1; i < s.length(); i++ ) {
            char c = s.charAt( i );
            if ( upper ) {
                if ( Character.isLowerCase( c ) ) {
                    sb.append( Character.toUpperCase( c ) );
                    upper = false;
                    continue;
                } else {
                    sb.append( '_' );
                }
            }
            upper = c == '_';
            if ( !upper ) {
                sb.append( c );
            }
        }
    }

    private static void appendUpperCamelCase(StringBuilder sb, String s) {
        sb.append( Character.toUpperCase( s.charAt( 0 ) ) );
        boolean upper = false;
        for ( int i = 1; i < s.length(); i++ ) {
            char c = s.charAt( i );
            if ( upper ) {
                if ( Character.isLowerCase( c ) ) {
                    sb.append( Character.toUpperCase( c ) );
                    upper = false;
                    continue;
                } else {
                    sb.append( '_' );
                }
            }
            upper = c == '_';
            if ( !upper ) {
                sb.append( c );
            }
        }
    }

    private static void writeMessage(Scope<DescriptorProto> scope,
                                     StringBuilder content, StringBuilder initializerContent) {
        content.append( "\timport com.netease.protobuf.*;\n" );
        content.append( "\timport flash.utils.Endian;\n" );
        content.append( "\timport flash.utils.IDataInput;\n" );
        content.append( "\timport flash.utils.IDataOutput;\n" );
        content.append( "\timport flash.errors.IOError;\n" );
        HashSet<String> importTypes = new HashSet<String>();
        for ( FieldDescriptorProto efdp : scope.proto.getExtensionList() ) {
            importTypes.add( scope.find( efdp.getExtendee() ).fullName );
            if ( efdp.getType().equals( FieldDescriptorProto.Type.TYPE_MESSAGE ) ) {
                importTypes.add( scope.find( efdp.getTypeName() ).fullName );
            }
            String importType = getImportType( scope, efdp );
            if ( importType != null ) {
                importTypes.add( importType );
            }
        }
        for ( FieldDescriptorProto fdp : scope.proto.getFieldList() ) {
            String importType = getImportType( scope, fdp );
            if ( importType != null ) {
                importTypes.add( importType );
            }
        }
        for ( String importType : importTypes ) {
            content.append( "\timport " );
            content.append( importType );
            content.append( ";\n" );
        }
        content.append( "\t// @@protoc_insertion_point(imports)\n\n" );
        if ( scope.proto.hasOptions() ) {
            String remoteClassAlias;
            if ( scope.proto.getOptions().hasExtension( Options.as3AmfAlias ) ) {
                remoteClassAlias = scope.proto.getOptions().getExtension( Options.as3AmfAlias );
            } else if ( scope.proto.getOptions().getExtension( Options.as3AmfAutoAlias ) ) {
                remoteClassAlias = scope.fullName;
            } else {
                remoteClassAlias = null;
            }
            if ( remoteClassAlias != null ) {
                content.append( "\t[RemoteClass(alias=" );
                appendQuotedString( content, remoteClassAlias );
                content.append( ")]\n" );
            }
            if ( scope.proto.getOptions().getExtension( Options.as3Bindable ) ) {
                content.append( "\t[Bindable]\n" );
            }
        }
        content.append( "\t// @@protoc_insertion_point(class_metadata)\n" );
        if ( scope.proto.getExtensionRangeCount() > 0 ) {
            content.append( "\tpublic dynamic final class " );
            content.append( scope.proto.getName() );
            content.append( " extends Array implements com.netease.protobuf.IMessage {\n" );
            content.append( "\t\t[ArrayElementType(\"Function\")]\n" );
            content.append( "\t\tpublic static const extensionWriteFunctions:Array = [];\n\n" );
            content.append( "\t\t[ArrayElementType(\"Function\")]\n" );
            content.append( "\t\tpublic static const extensionReadFunctions:Array = [];\n\n" );
        } else {
            content.append( "\tpublic final class " );
            content.append( scope.proto.getName() );
            content.append( " implements com.netease.protobuf.IMessage {\n" );
        }
        content.append( "\t\tpublic function " ).append( scope.proto.getName() ).append( "() {}\n" );
        for ( FieldDescriptorProto efdp : scope.proto.getExtensionList() ) {
            initializerContent.append( "import " );
            initializerContent.append( scope.fullName );
            initializerContent.append( ";\n" );
            initializerContent.append( "void(" );
            initializerContent.append( scope.fullName );
            initializerContent.append( "." );
            appendLowerCamelCase( initializerContent, efdp.getName() );
            initializerContent.append( ");\n" );
            String extendee = scope.find( efdp.getExtendee() ).fullName;
            content.append( "\t\tpublic static const " );
            appendLowerCamelCase( content, efdp.getName() );
            content.append( ":uint = " );
            content.append( efdp.getNumber() );
            content.append( ";\n\n" );
            content.append( "\t\t{\n" );
            content.append( "\t\t\t" );
            content.append( extendee );
            content.append( ".extensionReadFunctions[" );
            appendLowerCamelCase( content, efdp.getName() );
            content.append( "] = " );
            appendReadFunction( content, scope, efdp );
            content.append( ";\n" );
            content.append( "\t\t\t" );
            content.append( extendee );
            content.append( ".extensionWriteFunctions[" );
            appendLowerCamelCase( content, efdp.getName() );
            content.append( "] = " );
            appendWriteFunction( content, scope, efdp );
            content.append( ";\n" );
            content.append( "\t\t}\n\n" );
        }
        int valueTypeCount = 0;
        for ( FieldDescriptorProto fdp : scope.proto.getFieldList() ) {
            if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP ) {
                System.err.println( "Warning: Group is not supported." );
                continue;
            }
            assert (fdp.hasLabel());
            switch( fdp.getLabel() ) {
                case LABEL_OPTIONAL:
                    content.append( "\t\tprivate var " );
                    content.append( fdp.getName() );
                    content.append( "$field:" );
                    content.append( getActionScript3Type( scope, fdp ) );
                    content.append( ";\n\n" );

                    if ( isValueType( fdp.getType() ) ) {
                        final int valueTypeId = valueTypeCount++;
                        final int valueTypeField = valueTypeId / 32;
                        final int valueTypeBit = valueTypeId % 32;
                        if ( valueTypeBit == 0 ) {
                            content.append( "\t\tprivate var hasField$" );
                            content.append( valueTypeField );
                            content.append( ":uint = 0;\n\n" );
                        }
                        content.append( "\t\tpublic function remove" );
                        appendUpperCamelCase( content, fdp.getName() );
                        content.append( "():void {\n" );
                        content.append( "\t\t\thasField$" );
                        content.append( valueTypeField );
                        content.append( " &= 0x" );
                        content.append( Integer.toHexString( ~(1 << valueTypeBit) ) );
                        content.append( ";\n" );

                        content.append( "\t\t\t" );
                        content.append( fdp.getName() );
                        content.append( "$field = " );
                        if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_ENUM ) {
                            if ( fdp.hasDefaultValue() ) {
                                String defaultValue = fdp.getDefaultValue();
                                content.append( getActionScript3Type( scope, fdp ) );
                                content.append( "." ).append( defaultValue );
                            } else {
                                content.append( "null" );
                            }
                            content.append( ";\n" );
                        } else {
                            content.append( "new " );
                            content.append( getActionScript3Type( scope, fdp ) );
                            content.append( "();\n" );
                        }
                        content.append( "\t\t}\n\n" );

                        content.append( "\t\tpublic function get has" );
                        appendUpperCamelCase( content, fdp.getName() );
                        content.append( "():Boolean {\n" );
                        content.append( "\t\t\treturn (hasField$" );
                        content.append( valueTypeField );
                        content.append( " & 0x" );
                        content.append( Integer.toHexString( 1 << valueTypeBit ) );
                        content.append( ") != 0;\n" );
                        content.append( "\t\t}\n\n" );

                        content.append( "\t\tpublic function set " );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( "(value:" );
                        content.append( getActionScript3Type( scope, fdp ) );
                        content.append( "):void {\n" );
                        content.append( "\t\t\thasField$" );
                        content.append( valueTypeField );
                        content.append( " |= 0x" );
                        content.append( Integer.toHexString( 1 << valueTypeBit ) );
                        content.append( ";\n" );
                        content.append( "\t\t\t" );
                        content.append( fdp.getName() );
                        content.append( "$field = value;\n" );
                        content.append( "\t\t}\n\n" );
                    } else {
                        content.append( "\t\tpublic function remove" );
                        appendUpperCamelCase( content, fdp.getName() );
                        content.append( "():void {\n" );
                        content.append( "\t\t\t" );
                        content.append( fdp.getName() );
                        content.append( "$field = null;\n" );
                        content.append( "\t\t}\n\n" );

                        content.append( "\t\tpublic function get has" );
                        appendUpperCamelCase( content, fdp.getName() );
                        content.append( "():Boolean {\n" );
                        content.append( "\t\t\treturn " );
                        content.append( fdp.getName() );
                        content.append( "$field != null;\n" );
                        content.append( "\t\t}\n\n" );

                        content.append( "\t\tpublic function set " );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( "(value:" );
                        content.append( getActionScript3Type( scope, fdp ) );
                        content.append( "):void {\n" );
                        content.append( "\t\t\t" );
                        content.append( fdp.getName() );
                        content.append( "$field = value;\n" );
                        content.append( "\t\t}\n\n" );
                    }

                    content.append( "\t\tpublic function get " );
                    appendLowerCamelCase( content, fdp.getName() );
                    content.append( "():" );
                    content.append( getActionScript3Type( scope, fdp ) );
                    content.append( " {\n" );
                    if ( fdp.hasDefaultValue() ) {
                        content.append( "\t\t\tif(!has" );
                        appendUpperCamelCase( content, fdp.getName() );
                        content.append( ") {\n" );
                        content.append( "\t\t\t\treturn " );
                        appendDefaultValue( content, scope, fdp );
                        content.append( ";\n" );
                        content.append( "\t\t\t}\n" );
                    }
                    content.append( "\t\t\treturn " );
                    content.append( fdp.getName() );
                    content.append( "$field;\n" );
                    content.append( "\t\t}\n\n" );
                    break;
                case LABEL_REQUIRED:
                    content.append( "\t\tpublic var " );
                    appendLowerCamelCase( content, fdp.getName() );
                    content.append( ":" );
                    content.append( getActionScript3Type( scope, fdp ) );
                    if ( fdp.hasDefaultValue() ) {
                        content.append( " = " );
                        appendDefaultValue( content, scope, fdp );
                    }
                    content.append( ";\n\n" );
                    break;
                case LABEL_REPEATED:
                    String type = getActionScript3Type( scope, fdp );
                    content.append( "\t\tpublic var " );
                    appendLowerCamelCase( content, fdp.getName() );
                    content.append( ":Vector.<" );
                    content.append( type );
                    content.append( "> = new Vector.<" );
                    content.append( type );
                    content.append( ">();\n\n" );
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        content.append( "\t\t/**\n\t\t *  @private\n\t\t */\n\t\tpublic final function writeToBuffer(output:com.netease.protobuf.WritingBuffer):void {\n" );
        for ( FieldDescriptorProto fdp : scope.proto.getFieldList() ) {
            if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP ) {
                System.err.println( "Warning: Group is not supported." );
                continue;
            }
            switch( fdp.getLabel() ) {
                case LABEL_OPTIONAL:
                    content.append( "\t\t\tif (" );
                    content.append( "has" );
                    appendUpperCamelCase( content, fdp.getName() );
                    content.append( ") {\n" );
                    content.append( "\t\t\t\tcom.netease.protobuf.WriteUtils.writeTag(output, com.netease.protobuf.WireType." );
                    content.append( getActionScript3WireType( fdp.getType() ) );
                    content.append( ", " );
                    content.append( Integer.toString( fdp.getNumber() ) );
                    content.append( ");\n" );
                    content.append( "\t\t\t\tcom.netease.protobuf.WriteUtils.write$" );
                    content.append( fdp.getType().name() );
                    content.append( "(output, " );
                    content.append( fdp.getName() );
                    content.append( "$field);\n" );
                    content.append( "\t\t\t}\n" );
                    break;
                case LABEL_REQUIRED:
                    content.append( "\t\t\tcom.netease.protobuf.WriteUtils.writeTag(output, com.netease.protobuf.WireType." );
                    content.append( getActionScript3WireType( fdp.getType() ) );
                    content.append( ", " );
                    content.append( Integer.toString( fdp.getNumber() ) );
                    content.append( ");\n" );
                    content.append( "\t\t\tcom.netease.protobuf.WriteUtils.write$" );
                    content.append( fdp.getType().name() );
                    content.append( "(output, " );
                    appendLowerCamelCase( content, fdp.getName() );
                    content.append( ");\n" );
                    break;
                case LABEL_REPEATED:
                    if ( fdp.hasOptions() && fdp.getOptions().getPacked() ) {
                        content.append( "\t\t\tif (" );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( " != null && " );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( ".length > 0) {\n" );
                        content.append( "\t\t\t\tcom.netease.protobuf.WriteUtils.writeTag(output, com.netease.protobuf.WireType.LENGTH_DELIMITED, " );
                        content.append( Integer.toString( fdp.getNumber() ) );
                        content.append( ");\n" );
                        content.append( "\t\t\t\tcom.netease.protobuf.WriteUtils.writePackedRepeated(output, com.netease.protobuf.WriteUtils.write$" );
                        content.append( fdp.getType().name() );
                        content.append( ", " );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( ");\n" );
                        content.append( "\t\t\t}\n" );
                    } else {
                        content.append( "\t\t\tfor (var " );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( "Index:uint = 0; " );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( "Index < " );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( ".length; ++" );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( "Index) {\n" );
                        content.append( "\t\t\t\tcom.netease.protobuf.WriteUtils.writeTag(output, com.netease.protobuf.WireType." );
                        content.append( getActionScript3WireType( fdp.getType() ) );
                        content.append( ", " );
                        content.append( Integer.toString( fdp.getNumber() ) );
                        content.append( ");\n" );
                        content.append( "\t\t\t\tcom.netease.protobuf.WriteUtils.write$" );
                        content.append( fdp.getType().name() );
                        content.append( "(output, " );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( "[" );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( "Index]);\n" );
                        content.append( "\t\t\t}\n" );
                    }
                    break;
            }
        }
        if ( scope.proto.getExtensionRangeCount() > 0 ) {
            content.append( "\t\t\tfor (var tagNumber:* in this) {\n" );
            content.append( "\t\t\t\tvar writeFunction:Function = extensionWriteFunctions[tagNumber];\n" );
            content.append( "\t\t\t\tif (writeFunction == null) {\n" );
            content.append( "\t\t\t\t\tthrow new flash.errors.IOError('Attemp to write an unknown field.')\n" );
            content.append( "\t\t\t\t}\n" );
            content.append( "\t\t\t\twriteFunction(output, this, tagNumber);\n" );
            content.append( "\t\t\t}\n" );
        }
        content.append( "\t\t}\n\n" );
        content.append( "\t\tpublic final function writeDelimitedTo(output:flash.utils.IDataOutput):void {\n" );
        content.append( "\t\t\tconst buffer:com.netease.protobuf.WritingBuffer = new com.netease.protobuf.WritingBuffer();\n" );
        content.append( "\t\t\tWriteUtils.write$TYPE_MESSAGE(buffer, this);\n" );
        content.append( "\t\t\tbuffer.toNormal(output);\n" );
        content.append( "\t\t}\n\n" );
        content.append( "\t\t/**\n\t\t *  @private\n\t\t */\n" );
        content.append( "\t\tpublic final function readFromSlice(input:flash.utils.IDataInput, bytesAfterSlice:uint):void {\n" );
        for ( FieldDescriptorProto fdp : scope.proto.getFieldList() ) {
            if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP ) {
                System.err.println( "Warning: Group is not supported." );
                continue;
            }
            switch( fdp.getLabel() ) {
                case LABEL_OPTIONAL:
                case LABEL_REQUIRED:
                    content.append( "\t\t\tvar " );
                    content.append( fdp.getName() );
                    content.append( "$count:uint = 0;\n" );
                    break;
            }
        }
        content.append( "\t\t\twhile (input.bytesAvailable > bytesAfterSlice) {\n" );
        content.append( "\t\t\t\tvar tag:uint = com.netease.protobuf.ReadUtils.read$TYPE_UINT32(input);\n" );
        content.append( "\t\t\t\tswitch (tag >>> 3) {\n" );
        for ( FieldDescriptorProto fdp : scope.proto.getFieldList() ) {
            if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP ) {
                System.err.println( "Warning: Group is not supported." );
                continue;
            }
            content.append( "\t\t\t\tcase " );
            content.append( Integer.toString( fdp.getNumber() ) );
            content.append( ":\n" );
            switch( fdp.getLabel() ) {
                case LABEL_OPTIONAL:
                case LABEL_REQUIRED:
                    content.append( "\t\t\t\t\tif (" );
                    content.append( fdp.getName() );
                    content.append( "$count != 0) {\n" );
                    content.append( "\t\t\t\t\t\tthrow new flash.errors.IOError('Bad data format: " );
                    content.append( scope.proto.getName() );
                    content.append( '.' );
                    appendLowerCamelCase( content, fdp.getName() );
                    content.append( " cannot be set twice.');\n" );
                    content.append( "\t\t\t\t\t}\n" );
                    content.append( "\t\t\t\t\t++" );
                    content.append( fdp.getName() );
                    content.append( "$count;\n" );
                    if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE ) {
                        content.append( "\t\t\t\t\t" );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( " = new " );
                        content.append( getActionScript3Type( scope, fdp ) );
                        content.append( "();\n" );
                        content.append( "\t\t\t\t\tcom.netease.protobuf.ReadUtils.read$TYPE_MESSAGE(input, " );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( ");\n" );
                    } else if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_ENUM ) {
                        content.append( "\t\t\t\t\t" );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( " = " ).append( getActionScript3Type( scope, fdp ) );
                        content.append( ".valuesById[com.netease.protobuf.ReadUtils.read$TYPE_ENUM(input)];\n" );
                    } else {
                        content.append( "\t\t\t\t\t" );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( " = com.netease.protobuf.ReadUtils.read$" );
                        content.append( fdp.getType().name() );
                        content.append( "(input);\n" );
                    }
                    break;
                case LABEL_REPEATED:
                    switch( fdp.getType() ) {
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
                            content.append( "\t\t\t\t\tif ((tag & 7) == com.netease.protobuf.WireType.LENGTH_DELIMITED) {\n" );
                            content.append( "\t\t\t\t\t\tcom.netease.protobuf.ReadUtils.readPackedRepeated(input, com.netease.protobuf.ReadUtils.read$" );
                            content.append( fdp.getType().name() );
                            content.append( ", Vector.<Object>(" );
                            appendLowerCamelCase( content, fdp.getName() );
                            content.append( "));\n" );
                            content.append( "\t\t\t\t\t\tbreak;\n" );
                            content.append( "\t\t\t\t\t}\n" );
                    }
                    if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE ) {
                        content.append( "\t\t\t\t\tconst " );
                        content.append( fdp.getName() );
                        content.append( "$element:" );
                        content.append( getActionScript3Type( scope, fdp ) );
                        content.append( " = new " );
                        content.append( getActionScript3Type( scope, fdp ) );
                        content.append( "();\n\t\t\t\t\t" );
                        content.append( "com.netease.protobuf.ReadUtils.read$TYPE_MESSAGE(input, " );
                        content.append( fdp.getName() );
                        content.append( "$element);\n\t\t\t\t\t" );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( ".push(" );
                        content.append( fdp.getName() );
                        content.append( "$element);\n" );
                    } else if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_ENUM ) {
                        content.append( "\t\t\t\t\t" );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( ".push(" );
                        content.append( getActionScript3Type( scope, fdp ) );
                        content.append( ".valuesById[com.netease.protobuf.ReadUtils.read$" );
                        content.append( fdp.getType().name() );
                        content.append( "(input)]);" );
                    } else {
                        content.append( "\t\t\t\t\t" );
                        appendLowerCamelCase( content, fdp.getName() );
                        content.append( ".push(com.netease.protobuf.ReadUtils.read$" );
                        content.append( fdp.getType().name() );
                        content.append( "(input));" );
                    }
                    break;
            }
            content.append( "\t\t\t\t\tbreak;\n" );
        }
        content.append( "\t\t\t\tdefault:\n" );
        if ( scope.proto.getExtensionRangeCount() > 0 ) {
            content.append( "\t\t\t\t\tvar readFunction:Function = extensionReadFunctions[tag >>> 3];\n" );
            content.append( "\t\t\t\t\tif (readFunction != null) {\n" );
            content.append( "\t\t\t\t\t\treadFunction(input, this, tag);\n" );
            content.append( "\t\t\t\t\t\tbreak;\n" );
            content.append( "\t\t\t\t\t}\n" );
        }
        content.append( "\t\t\t\t\tcom.netease.protobuf.ReadUtils.skip(input, tag & 7);\n" );
        content.append( "\t\t\t\t}\n" );
        content.append( "\t\t\t}\n" );
        for ( FieldDescriptorProto fdp : scope.proto.getFieldList() ) {
            if ( fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP ) {
                System.err.println( "Warning: Group is not supported." );
                continue;
            }
            switch( fdp.getLabel() ) {
                case LABEL_REQUIRED:
                    content.append( "\t\t\tif (" );
                    content.append( fdp.getName() );
                    content.append( "$count != 1) {\n" );
                    content.append( "\t\t\t\tthrow new flash.errors.IOError('Bad data format: " );
                    content.append( scope.proto.getName() );
                    content.append( '.' );
                    appendLowerCamelCase( content, fdp.getName() );
                    content.append( " must be set.');\n" );
                    content.append( "\t\t\t}\n" );
                    break;
            }
        }
        content.append( "\t\t}\n\n" );
        content.append( "\t\tpublic final function mergeDelimitedFrom(input:IDataInput):void {\n" );
        content.append( "\t\t\tinput.endian = flash.utils.Endian.LITTLE_ENDIAN;\n" );
        content.append( "\t\t\tReadUtils.read$TYPE_MESSAGE(input, this);\n" );
        content.append( "\t\t}\n\n" );
        content.append( "\t\tpublic function toString():String {\n" );
        content.append( "\t\t	return messageToString(this);\n" );
        content.append( "\t\t}\n\n" );
        content.append( "\t}\n" );
    }

    private static void writeExtension(Scope<FieldDescriptorProto> scope,
                                       StringBuilder content, StringBuilder initializerContent) {
        initializerContent.append( "import " );
        initializerContent.append( scope.fullName );
        initializerContent.append( ";\n" );
        initializerContent.append( "void(" );
        initializerContent.append( scope.fullName );
        initializerContent.append( ");\n" );
        content.append( "\timport com.netease.protobuf.*;\n" );
        if ( scope.proto.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE ) {
            content.append( "\timport " );
            content.append(
                    scope.parent.find( scope.proto.getTypeName() ).fullName );
            content.append( ";\n" );
        }
        String extendee = scope.parent.find( scope.proto.getExtendee() ).fullName;
        content.append( "\timport " );
        content.append( extendee );
        content.append( ";\n" );
        content.append( "\tpublic const " );
        appendLowerCamelCase( content, scope.proto.getName() );
        content.append( ":uint = " );
        content.append( scope.proto.getNumber() );
        content.append( ";\n" );
        content.append( "\t{\n" );
        content.append( "\t\t" );
        content.append( extendee );
        content.append( ".extensionReadFunctions[" );
        appendLowerCamelCase( content, scope.proto.getName() );
        content.append( "] = " );
        appendReadFunction( content, scope.parent, scope.proto );
        content.append( ";\n" );
        content.append( "\t\t" );
        content.append( extendee );
        content.append( ".extensionWriteFunctions[" );
        appendLowerCamelCase( content, scope.proto.getName() );
        content.append( "] = " );
        appendWriteFunction( content, scope.parent, scope.proto );
        content.append( ";\n" );
        content.append( "\t}\n" );
    }

    private static void writeEnum(Scope<EnumDescriptorProto> scope,
                                  StringBuilder content) {
        content.append( "\timport com.netease.protobuf.Enum;\n" );
        content.append( "\tpublic final class " );
        content.append( scope.proto.getName() );
        content.append( " implements com.netease.protobuf.Enum {\n" );
        for ( EnumValueDescriptorProto evdp : scope.proto.getValueList() ) {
            content.append( "\t\tpublic static const " );
            content.append( evdp.getName() );
            content.append( ":" ).append( scope.proto.getName() );
            content.append( " = new " ).append( scope.proto.getName() );
            content.append( "(" );
            content.append( evdp.getNumber() ).append( ", \"" );
            content.append( evdp.getName() );
            content.append( "\")" );
            content.append( ";\n" );
        }
        content.append( "\t\tprivate var _id:int;\n" );
        content.append( "\t\tprivate var _name:String;\n" );
        content.append( "\t\tpublic function " ).append( scope.proto.getName() ).append( "(id:int, name:String) {\n" );
        content.append( "\t\t\tthis._id=id\n" );
        content.append( "\t\t\tthis._name=name\n" );
        content.append( "\t\t}\n\n" );
        content.append( "\t\tpublic function id():int {\n" );
        content.append( "\t\t\treturn _id;\n" );
        content.append( "\t\t}\n" );
        content.append( "\t\tpublic function name():String {\n" );
        content.append( "\t\t\treturn _name;\n" );
        content.append( "\t\t}\n" );
        {
            content.append( "\t\tpublic static const values : Vector.<" )
                    .append( scope.proto.getName() )
                    .append( "> = Vector.<" )
                    .append( scope.proto.getName() ).append( ">([" );
            boolean first = true;
            for ( EnumValueDescriptorProto evdp : scope.proto.getValueList() ) {
                if ( !first ) {
                    content.append( "," );
                } else {
                    first = false;
                }
                content.append( "\n" );
                content.append( "\t\t\t" );
                content.append( evdp.getName() );
            }
            content.append( "\n\t\t]);\n" );
        }
        {
            content.append( "\t\tpublic static const valuesById : Object = {};" );
            for ( EnumValueDescriptorProto evdp : scope.proto.getValueList() ) {
                content.append( "\n" );
                content.append( "\t\t" );
                content.append( "valuesById[" );
                content.append( evdp.getNumber() );
                content.append( "]" );
                content.append( " = " );
                content.append( evdp.getName() );
                content.append( ";" );
            }
        }
        content.append( "\t}\n" );
    }

    @SuppressWarnings( "unchecked" )
    private static void writeFile(Scope<?> scope, StringBuilder content,
                                  StringBuilder initializerContent) {
        content.append( "package " );
        content.append( scope.parent.fullName );
        content.append( " {\n" );
        if ( scope.proto instanceof DescriptorProto ) {
            writeMessage( (Scope<DescriptorProto>) scope, content,
                    initializerContent );
        } else if ( scope.proto instanceof ServiceDescriptorProto ) {
            writeService( (Scope<ServiceDescriptorProto>) scope, content );
        } else if ( scope.proto instanceof EnumDescriptorProto ) {
            writeEnum( (Scope<EnumDescriptorProto>) scope, content );
        } else if ( scope.proto instanceof FieldDescriptorProto ) {
            Scope<FieldDescriptorProto> fdpScope =
                    (Scope<FieldDescriptorProto>) scope;
            if ( fdpScope.proto.getType() ==
                    FieldDescriptorProto.Type.TYPE_GROUP ) {
                System.err.println( "Warning: Group is not supported." );
            } else {
                writeExtension( fdpScope, content, initializerContent );
            }
        } else {
            throw new IllegalArgumentException();
        }
        content.append( "}\n" );
    }

    private static void writeFiles(Scope<?> root,
                                   CodeGeneratorResponse.Builder responseBuilder,
                                   StringBuilder initializerContent) {
        for ( Map.Entry<String, Scope<?>> entry : root.children.entrySet() ) {
            Scope<?> scope = entry.getValue();
            if ( scope.export ) {
                StringBuilder content = new StringBuilder();
                writeFile( scope, content, initializerContent );
                responseBuilder.addFile(
                        CodeGeneratorResponse.File.newBuilder().
                                setName( scope.fullName.replace( '.', '/' ) + ".as" ).
                                setContent( content.toString() ).
                                build()
                );
            }
            writeFiles( scope, responseBuilder, initializerContent );
        }
    }

    private static void writeFiles(Scope<?> root,
                                   CodeGeneratorResponse.Builder responseBuilder) {
        StringBuilder initializerContent = new StringBuilder();
        initializerContent.append( "{\n" );
        writeFiles( root, responseBuilder, initializerContent );
        initializerContent.append( "}\n" );
        responseBuilder.addFile(
                CodeGeneratorResponse.File.newBuilder().
                        setName( "initializer.as.inc" ).
                        setContent( initializerContent.toString() ).
                        build()
        );
    }

    private static void writeService(Scope<ServiceDescriptorProto> scope,
                                     StringBuilder content) {
        HashSet<String> importTypes = new HashSet<String>();
        for ( MethodDescriptorProto mdp : scope.proto.getMethodList() ) {
            importTypes.add( scope.find( mdp.getInputType() ).fullName );
            importTypes.add( scope.find( mdp.getOutputType() ).fullName );
        }
        for ( String importType : importTypes ) {
            content.append( "\timport " );
            content.append( importType );
            content.append( ";\n" );
        }
        content.append( "\t// @@protoc_insertion_point(imports)\n\n" );
        content.append( "\tpublic final class " );
        content.append( scope.proto.getName() );
        content.append( " {\n" );
        content.append( "\t\tpublic var sendFunction:Function;\n\n" );
        for ( MethodDescriptorProto mdp : scope.proto.getMethodList() ) {
            content.append( "\t\tpublic function " );
            appendLowerCamelCase( content, mdp.getName() );
            content.append( "(input:" );
            content.append( scope.find( mdp.getInputType() ).fullName );
            content.append( ", rpcResult:Function):void {\n" );
            content.append( "\t\t\tsendFunction(\"" );
            content.append( scope.fullName );
            content.append( "." );
            content.append( mdp.getName() );
            content.append( "\", input, rpcResult, " );
            content.append( scope.find( mdp.getOutputType() ).fullName );
            content.append( ");\n" );
            content.append( "\t\t}\n\n" );
        }
        content.append( "\t}\n" );

    }

    public static void main(String[] args) throws IOException {
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        //Options.registerAllExtensions(registry);
        CodeGeneratorRequest request = CodeGeneratorRequest.
                parseFrom( System.in, registry );
        CodeGeneratorResponse response;
        try {
            Scope<Object> root = buildScopeTree( request );
            CodeGeneratorResponse.Builder responseBuilder =
                    CodeGeneratorResponse.newBuilder();
            writeFiles( root, responseBuilder );
            response = responseBuilder.build();
        } catch( Exception e ) {
            // 出错，报告给 protoc ，然后退出
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter( sw );
            e.printStackTrace( pw );
            pw.flush();
            CodeGeneratorResponse.newBuilder().setError( sw.toString() ).
                    build().writeTo( System.out );
            System.out.flush();
            return;
        }
        response.writeTo( System.out );
        System.out.flush();
    }
}