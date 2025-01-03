package com.github.link2fun.setterpilot.services;

import com.github.link2fun.setterpilot.MyBundle;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class MyProjectService {

    private static final Logger LOG = Logger.getInstance(MyProjectService.class);

    public MyProjectService(Project project) {
        LOG.info(MyBundle.message("projectService", project.getName()));
        LOG.warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.");
    }

    public int getRandomNumber() {
        return (int) (Math.random() * 100) + 1;
    }
}