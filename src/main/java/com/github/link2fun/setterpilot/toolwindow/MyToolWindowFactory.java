package com.github.link2fun.setterpilot.toolwindow;

import com.github.link2fun.setterpilot.MyBundle;
import com.github.link2fun.setterpilot.services.MyProjectService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MyToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOG = Logger.getInstance(MyToolWindowFactory.class);

    public MyToolWindowFactory() {
        LOG.warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.");
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        MyToolWindow myToolWindow = new MyToolWindow(toolWindow);
        Content content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false);
        toolWindow.getContentManager().addContent(content);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }

    public static class MyToolWindow {

        private final MyProjectService service;

        public MyToolWindow(ToolWindow toolWindow) {
            this.service = toolWindow.getProject().getService(MyProjectService.class);
        }

        public JPanel getContent() {
            JBPanel<JBPanel<?>> panel = new JBPanel<>();
            JBLabel label = new JBLabel(MyBundle.message("randomLabel", "?"));

            panel.add(label);
            JButton shuffleBtn = new JButton(MyBundle.message("shuffle"));
            shuffleBtn.addActionListener(e -> label.setText(MyBundle.message("randomLabel", service.getRandomNumber())));
            panel.add(shuffleBtn);

            return panel;
        }
    }
}