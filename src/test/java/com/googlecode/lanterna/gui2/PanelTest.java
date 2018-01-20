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

import java.io.IOException;

public class PanelTest extends TestBase {
    public static void main(String[] args) throws IOException, InterruptedException {
        new PanelTest().run(args);
    }

    @Override
    public void init(WindowBasedTextGUI textGUI) {
        final BasicWindow window = new BasicWindow("Grid layout test");

        Panel mainPanel = new Panel();
        mainPanel.setLayoutManager(new LinearLayout(Direction.HORIZONTAL));

        Panel leftPanel = new Panel();
        mainPanel.addComponent(leftPanel.withBorder(Borders.INSTANCE.singleLine("Left")));

        Panel panel = new Panel();
        panel.addComponent(new Button("Panel 1 Button"));
        leftPanel.addComponent(panel.withBorder(Borders.singleLine()));
        panel = new Panel();
        panel.addComponent(new Button("Panel 2 Button"));
        leftPanel.addComponent(panel.withBorder(Borders.INSTANCE.singleLine("Title")));
        panel = new Panel();
        panel.addComponent(new Button("Panel 3 Button"));
        leftPanel.addComponent(panel.withBorder(Borders.doubleLine()));
        panel = new Panel();
        panel.addComponent(new Button("Panel 4 Button"));
        leftPanel.addComponent(panel.withBorder(Borders.INSTANCE.doubleLine("Title")));

        Panel rightPanel = new Panel();
        mainPanel.addComponent(rightPanel.withBorder(Borders.INSTANCE.singleLine("Right")));

        panel = new Panel();
        panel.addComponent(new Button("Panel 1 Button"));
        panel.addComponent(new Panel().withBorder(Borders.INSTANCE.singleLine("A")));
        panel.addComponent(new Panel().withBorder(Borders.INSTANCE.singleLine("Some Text")));
        rightPanel.addComponent(panel.withBorder(Borders.INSTANCE.singleLine("B")));
        panel = new Panel();
        panel.addComponent(new Button("Panel 2 Button"));
        rightPanel.addComponent(panel.withBorder(Borders.INSTANCE.singleLine("Title")));
        panel = new Panel();
        panel.addComponent(new Button("Panel 3 Button"));
        rightPanel.addComponent(panel.withBorder(Borders.doubleLine()));
        panel = new Panel();
        panel.addComponent(new Button("Panel 4 Button"));
        rightPanel.addComponent(panel.withBorder(Borders.INSTANCE.doubleLine("Title")));

        window.setComponent(Panels.INSTANCE.vertical(
                mainPanel.withBorder(Borders.INSTANCE.singleLine("Main")),
                new Button("OK", new Runnable() {
                    @Override
                    public void run() {
                        window.close();
                    }
                })));
        mainPanel.getPreferredSize();
        textGUI.addWindow(window);
    }
}
