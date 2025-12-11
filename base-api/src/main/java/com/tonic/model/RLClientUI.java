package com.tonic.model;

import com.tonic.Static;
import com.tonic.util.ReflectBuilder;
import lombok.SneakyThrows;

import javax.swing.*;

public class RLClientUI
{
    private final Object clientUI;
    RLClientUI(Class<?> main)
    {
        this.clientUI = ReflectBuilder
                .of(main)
                .staticField("rlInstance")
                .field("clientUI")
                .get();
    }

    public void removeNavigation(final Object button)
    {
        SwingUtilities.invokeLater(() -> ReflectBuilder
                .of(clientUI)
                .method("removeNavigation", new Class<?>[]{getNavButtonClass()}, new Object[]{button})
                .get()
        );
    }

    public void addNavigation(final Object button)
    {
        SwingUtilities.invokeLater(() -> ReflectBuilder
                .of(clientUI)
                .method("addNavigation", new Class<?>[]{getNavButtonClass()}, new Object[]{button})
                .get()
        );
    }

    @SneakyThrows
    private Class<?> getNavButtonClass()
    {
        return Static.getRuneLite()
                .getRuneLiteMain()
                .getClassLoader()
                .loadClass("net.runelite.client.ui.NavigationButton");
    }
}
