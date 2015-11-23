package com.netease.protocGenAs3;

import com.google.protobuf.ExtensionRegistry;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static com.google.protobuf.DescriptorProtos.*;
import static google.protobuf.compiler.Plugin.CodeGeneratorRequest;
import static google.protobuf.compiler.Plugin.CodeGeneratorResponse;

public final class Main {
    private static void addEnumToScope(Scope<?> scope, EnumDescriptorProto edp,
                                       boolean export) {
        assert (edp.hasName());
        Scope<EnumDescriptorProto> enumScope = scope.addChild(edp.getName(), edp, export);
        for (EnumValueDescriptorProto evdp : edp.getValueList()) {
            Scope<EnumValueDescriptorProto> enumValueScope =
                    enumScope.addChild(evdp.getName(), evdp, false);
            scope.addChild(evdp.getName(), enumValueScope, false);
        }
    }

    private static void addMessageToScope(Scope<?> scope, DescriptorProto dp,
                                          boolean export) {
        Scope<DescriptorProto> messageScope = scope.addChild(dp.getName(), dp, export);
        for (EnumDescriptorProto edp : dp.getEnumTypeList()) {
            addEnumToScope(messageScope, edp, export);
        }
        for (DescriptorProto nested : dp.getNestedTypeList()) {
            addMessageToScope(messageScope, nested, export);
        }
    }

    private static Scope<Object> buildScopeTree(CodeGeneratorRequest request) {
        Scope<Object> root = Scope.root();
        List<String> filesToGenerate = request.getFileToGenerateList();
        for (FileDescriptorProto fdp : request.getProtoFileList()) {
            Scope<?> packageScope = fdp.hasPackage() ? root.findOrCreate(fdp.getPackage()) : root;
            boolean export = filesToGenerate.contains(fdp.getName());

            for (EnumDescriptorProto edp : fdp.getEnumTypeList()) {
                addEnumToScope(packageScope, edp, export);
            }

            for (DescriptorProto dp : fdp.getMessageTypeList()) {
                addMessageToScope(packageScope, dp, export);
            }
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static void writeFile(Scope<?> scope, StringBuilder content) {
        content.append("package " + scope.parent.fullName + " {\n");
        if (scope.proto instanceof DescriptorProto) {
            MessageWriter.writeMessage((Scope<DescriptorProto>) scope, content);
        } else if (scope.proto instanceof EnumDescriptorProto) {
            EnumWriter.writeEnum((Scope<EnumDescriptorProto>) scope, content);
        } else {
            throw new IllegalArgumentException("not supported");
        }
        content.append("}\n");
    }

    private static void writeFiles(Scope<?> root,
                                   CodeGeneratorResponse.Builder responseBuilder) {
        for (Map.Entry<String, Scope<?>> entry : root.children.entrySet()) {
            Scope<?> scope = entry.getValue();
            if (scope.export) {
                StringBuilder content = new StringBuilder();
                writeFile(scope, content);
                responseBuilder.addFile(
                        CodeGeneratorResponse.File.newBuilder().
                                setName(scope.fullName.replace('.', '/') + ".as").
                                setContent(content.toString()).
                                build()
                );
            }
            writeFiles(scope, responseBuilder);
        }
    }

    public static void main(String[] args) throws IOException {
        ExtensionRegistry registry = ExtensionRegistry.newInstance();
        //Options.registerAllExtensions(registry);
        CodeGeneratorRequest request = CodeGeneratorRequest.
                parseFrom(System.in, registry);
        CodeGeneratorResponse response;
        try {
            Scope<Object> root = buildScopeTree(request);
            CodeGeneratorResponse.Builder responseBuilder =
                    CodeGeneratorResponse.newBuilder();
            writeFiles(root, responseBuilder);
            response = responseBuilder.build();
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
            CodeGeneratorResponse.newBuilder().setError(sw.toString()).
                    build().writeTo(System.out);
            System.out.flush();
            return;
        }
        response.writeTo(System.out);
        System.out.flush();
    }
}
