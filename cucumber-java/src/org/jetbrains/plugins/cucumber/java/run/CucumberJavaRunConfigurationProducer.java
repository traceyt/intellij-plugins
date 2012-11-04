package org.jetbrains.plugins.cucumber.java.run;

import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.Location;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.JavaRuntimeConfigurationProducerBase;
import com.intellij.execution.junit2.info.LocationUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;

/**
 * @author Andrey.Vokin
 * @since 8/6/12
 */
public abstract class CucumberJavaRunConfigurationProducer extends JavaRuntimeConfigurationProducerBase implements Cloneable {
  public static final String FORMATTER_OPTIONS = " --format org.jetbrains.plugins.cucumber.java.run.CucumberJvmSMFormatter --monochrome";
  public static final String CUCUMBER_MAIN_CLASS = "cucumber.cli.Main";
  protected PsiElement mySourceElement;

  protected CucumberJavaRunConfigurationProducer() {
    super(CucumberJavaRunConfigurationType.getInstance());
  }

  protected abstract String getGlue();

  protected abstract String getName();

  @NotNull
  protected abstract VirtualFile getFileToRun();

  @Override
  public PsiElement getSourceElement() {
    return mySourceElement;
  }

  @Override
  protected RunnerAndConfigurationSettings createConfigurationByElement(Location location, ConfigurationContext context) {
    if (!LocationUtil.isJarAttached(location, "cucumber.cli.Main", new PsiDirectory[0])) return null;
    mySourceElement = location.getPsiElement();
    if (!isApplicable(location.getPsiElement(), context.getModule())) {
      return null;
    }

    return createConfiguration(location, context);
  }

  @Override
  public int compareTo(Object o) {
    return PREFERED;
  }

  protected boolean isApplicable(PsiElement locationElement, final Module module) {
    return locationElement != null && locationElement.getContainingFile() instanceof GherkinFile;
  }

  protected void processConfiguration(@NotNull final CucumberJavaRunConfiguration configuration) {
  }

  protected RunnerAndConfigurationSettings createConfiguration(Location location, ConfigurationContext context) {
    final Project project = context.getProject();
    final RunnerAndConfigurationSettings settings = cloneTemplateConfiguration(project, context);
    final CucumberJavaRunConfiguration configuration = (CucumberJavaRunConfiguration)settings.getConfiguration();

    final VirtualFile file = getFileToRun();
    final String glue = getGlue();
    configuration.setProgramParameters(file.getPath() + glue + FORMATTER_OPTIONS);
    if (StringUtil.isEmpty(configuration.MAIN_CLASS_NAME)) {
      configuration.MAIN_CLASS_NAME = CUCUMBER_MAIN_CLASS;
    }

    configuration.setName(getName());
    processConfiguration(configuration);

    setupConfigurationModule(context, configuration);
    JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location);
    return settings;
  }
}
