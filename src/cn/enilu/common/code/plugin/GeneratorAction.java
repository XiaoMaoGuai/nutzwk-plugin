package cn.enilu.common.code.plugin;

import cn.enilu.common.code.plugin.ui.ErrorDialog;
import cn.enilu.common.code.plugin.ui.ConfigDialog;
import cn.enilu.common.code.plugin.utils.GenerateConfig;
import com.google.common.io.Files;
import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.io.File;
import java.nio.charset.Charset;


/**
 * 入口类<br>
 * </p> Copyright by easecredit.com<br>
 * 作者: zhangtao <br>
 * 创建日期: 16-7-24<br>
 */
public class GeneratorAction  extends AnAction {
    Project project=null;
    public void actionPerformed(AnActionEvent e) {
        project = e.getData(CommonDataKeys.PROJECT);
        String basePath = project.getBasePath();
        String pomFile = basePath+"/pom.xml";
        try {
            String pomTxt = Files.toString(new File(pomFile), Charset.forName("utf8"));
            if(!pomTxt.contains("cn.enilu.tools")||!pomTxt.contains("nutzwk-code-generator")){
                new ErrorDialog(project,"没有添加nutzwk-code-generator依赖","请在pom.xml文件中添加依赖：\r\n" +
                        "        <dependency>\n" +
                        "            <groupId>cn.enilu.tools</groupId>\n" +
                        "            <artifactId>nutzwk-code-generator</artifactId>\n" +
                        "            <version>1.0</version>\n" +
                        "        </dependency>").show();
                return ;
            }
        }catch (Exception ex){
            new ErrorDialog(project,"error","该项目不是一个有效的maven项目，暂时不支持使用插件\n生成代码，可以通过该链接：\n" +
                    "http://mvnrepository.com/artifact/cn.enilu.tools/nutzwk-code-generator\n" +
                    "下载nutzwk-code-generatorjar的包添加到项目中，\n" +
                    "然后使用命令行生成代码").show();
            return ;
        }
        DataContext dataContext = e.getDataContext();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getDataContext();
        final Document document = editor.getDocument();

        PsiClass rootClass = getTargetClass(e, editor);
        if (null == rootClass) {
            new ErrorDialog(project).show();
            return;
        }

        ConfigDialog fieldDialog = new ConfigDialog(rootClass);
        fieldDialog.show();
        if (!fieldDialog.isOK()) {
            return;
        }


        generateCode(rootClass, fieldDialog.getGenerateConfig());
    }

    private void generateCode(final PsiClass rootClass,
                              final GenerateConfig generateConfig) {
        RunManager runManager = RunManager.getInstance(project);
        ApplicationConfiguration appConfig = new ApplicationConfiguration("generator", project, ApplicationConfigurationType.getInstance());
        appConfig.MAIN_CLASS_NAME = "cn.enilu.common.code.Generator";
        String entityClassName = rootClass.getName();
        StringBuilder programArgs = new StringBuilder();
        programArgs.append("-i ").append(entityClassName).append(" -u ").append(generateConfig.getBaseUri()).append(" -p ")
                .append(generateConfig.getBasePackage()) .append(generateConfig.isForce() ? " -f" : "")
                .append(generateConfig.getPages().length()>1?(" -v "+generateConfig.getPages()):"")
                .append(generateConfig.isConroller() ? " controller" : "")
                .append(generateConfig.isService() ? " service" : "").append(generateConfig.isView()?" view":"") ;

        appConfig.PROGRAM_PARAMETERS = programArgs.toString();

        appConfig.WORKING_DIRECTORY = project.getBasePath();
        Module[] modules = ModuleManager.getInstance(project).getModules();

        appConfig.setModule(modules[0]);
        RunnerAndConfigurationSettings configuration = runManager.createConfiguration(appConfig, appConfig.getFactory());
        runManager.addConfiguration(configuration, true);
        Executor executor = ExecutorRegistry.getInstance().getExecutorById(com.intellij.openapi.wm.ToolWindowId.DEBUG);
        ExecutionUtil.runConfiguration(configuration, executor);

    }

    private PsiClass getTargetClass(AnActionEvent e, Editor editor) {
        final PsiFile file = e.getData(LangDataKeys.PSI_FILE);
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        PsiClass psiClass =  PsiTreeUtil.getParentOfType(element, PsiClass.class);

        return psiClass;
    }

    @Override
    public void update(AnActionEvent e) {
        final Project project = e.getData(CommonDataKeys.PROJECT);
        final Editor editor = e.getData(CommonDataKeys.EDITOR);
        final PsiClass psiClass = getTargetClass(e, editor);
        e.getPresentation().setVisible((null != project && null != editor && null != psiClass &&
                !psiClass.isEnum() && 0 != psiClass.getAllFields().length));
    }

}
