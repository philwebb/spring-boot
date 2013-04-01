package org.springframework.bootstrap.shell;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;


public class OpenEditor {

	public static void main(String[] args) {
		try {
			Desktop.getDesktop().edit(new File("src/main/resources/Example.groovy"));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
