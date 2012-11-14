package org.springframework.bootstrap;

import java.io.PrintStream;


class Banner {

	private static final String[] BANNER = {
"  ____          _            ____           _       _                  __ __ __    ",
" / ___'_ __ _ _(_)_ __  __ _| __ ) ___  ___| |_ ___| |_ _ _ __ _ _ __  \\ \\\\ \\\\ \\   ",
" \\___ | '_ | '_| | '_ \\/ _` |  _ \\/ _ \\/ _ | __/ __| __| '_/ _` | '_ \\  \\ \\\\ \\\\ \\  ",
"  ___)| |_)| | | | | || (_| | |_)| (_)| (_)| |_\\__ | |_| || (_| | |_) |  } }} }} } ",
" |____| .__|_| |_|_| |_\\__, |____/\\___/\\___/\\__|___/\\__|_| \\__,_| .__/  / // // /  ",
"      |_|              |___/                                    |_|    /_//_//_/   "};
	public static void write(PrintStream printStream) {
		for (String line : BANNER) {
			printStream.println(line);
		}
	}


}
