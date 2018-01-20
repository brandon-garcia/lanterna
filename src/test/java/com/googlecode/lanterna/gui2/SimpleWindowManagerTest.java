/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2017 Martin Berglund
 */
package com.googlecode.lanterna.gui2;

import com.googlecode.lanterna.TestUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;

/**
 * Test/example class for various kinds of window manager behaviours
 * @author Martin
 */
public class SimpleWindowManagerTest extends TestBase {
    public static void main(String[] args) throws IOException, InterruptedException {
        new SimpleWindowManagerTest().run(args);
    }

    @Override
    public void init(final WindowBasedTextGUI textGUI) {
        final Window mainWindow = new BasicWindow("Choose test");
        Panel contentArea = new Panel();
        contentArea.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        contentArea.addComponent(new Button("Centered window", new Runnable() {
            @Override
            public void run() {
                textGUI.addWindow(new CenteredWindow());
            }
        }));
        contentArea.addComponent(new Button("Undecorated window", new Runnable() {
            @Override
            public void run() {
                textGUI.addWindow(new UndecoratedWindow());
            }
        }));
        contentArea.addComponent(new Button("Undecorated + Centered window", new Runnable() {
            @Override
            public void run() {
                textGUI.addWindow(new UndecoratedCenteredWindow());
            }
        }));
        contentArea.addComponent(new Button("Full-screen window", new Runnable() {
            @Override
            public void run() {
                textGUI.addWindow(new FullScreenWindow(true));
            }
        }));
        contentArea.addComponent(new Button("Undecorated + Full-screen window", new Runnable() {
            @Override
            public void run() {
                textGUI.addWindow(new FullScreenWindow(false));
            }
        }));
        contentArea.addComponent(new Button("Expanded window", new Runnable() {
            @Override
            public void run() {
                textGUI.addWindow(new ExpandedWindow(true));
            }
        }));
        contentArea.addComponent(new Button("Undecorated + Expanded window", new Runnable() {
            @Override
            public void run() {
                textGUI.addWindow(new ExpandedWindow(false));
            }
        }));
        contentArea.addComponent(new Button("Close", new Runnable() {
            @Override
            public void run() {
                mainWindow.close();
            }
        }));
        mainWindow.setComponent(contentArea);
        textGUI.addWindow(mainWindow);
    }

    private static class CenteredWindow extends TestWindow {
        CenteredWindow() {
            super("Centered window");
            setHints(Collections.singletonList(Hint.Companion.getCENTERED()));
        }
    }

    private static class UndecoratedWindow extends TestWindow {
        UndecoratedWindow() {
            super("Undecorated");
            setHints(Collections.singletonList(Hint.Companion.getNO_DECORATIONS()));
        }
    }

    private class UndecoratedCenteredWindow extends TestWindow {

        UndecoratedCenteredWindow() {
            super("UndecoratedCentered");
            setHints(Arrays.asList(Hint.Companion.getNO_DECORATIONS(), Hint.Companion.getCENTERED()));
        }
    }

    private class FullScreenWindow extends TestWindow {
        private final boolean decorations;

        public FullScreenWindow(boolean decorations) {
            super("FullScreenWindow");
            this.decorations = decorations;

            Panel content = new Panel();
            content.setLayoutManager(new BorderLayout());
            TextBox textBox = new TextBox(TestUtils.downloadGPL(), TextBox.Style.MULTI_LINE);
            textBox.setLayoutData(BorderLayout.Location.CENTER);
            textBox.setReadOnly(true);
            content.addComponent(textBox);

            setComponent(content);

            setHints(this.decorations ? Collections.singletonList(Hint.Companion.getFULL_SCREEN()) : Arrays.asList(Hint.Companion.getFULL_SCREEN(), Hint.Companion.getNO_DECORATIONS()));
        }
    }

    private class ExpandedWindow extends TestWindow {
        private final boolean decorations;

        public ExpandedWindow(boolean decorations) {
            super("ExpandedWindow");
            this.decorations = decorations;

            Panel content = new Panel();
            content.setLayoutManager(new BorderLayout());
            TextBox textBox = new TextBox(TestUtils.downloadGPL(), TextBox.Style.MULTI_LINE);
            textBox.setLayoutData(BorderLayout.Location.CENTER);
            textBox.setReadOnly(true);
            content.addComponent(textBox);

            setComponent(content);

            setHints(this.decorations ? Collections.singletonList(Hint.Companion.getEXPANDED()) : Arrays.asList(Hint.Companion.getEXPANDED(), Hint.Companion.getNO_DECORATIONS()));
        }
    }

    private static class TestWindow extends BasicWindow {
        TestWindow(String title) {
            super(title);
            setComponent(new Button("Close", new Runnable() {
                @Override
                public void run() {
                    close();
                }
            }));
            setCloseWindowWithEscape(true);
        }
    }
}
