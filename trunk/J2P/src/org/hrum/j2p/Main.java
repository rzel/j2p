package org.hrum.j2p;

import japa.parser.JavaParser;
import japa.parser.ast.CompilationUnit;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Main {

	private File output = null;

	private List<File> inputs = new ArrayList<File>();

	private void addInputFiles(File dir) {
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			addInputFile(files[i]);
		}
	}

	private void addInputFile(File f) {
		if (f.isDirectory()) {
			addInputFiles(f);
		} else {
			if (f.getName().endsWith(".java")) {
				this.inputs.add(f);
			}
		}
	}

	private boolean ensureDir(File dir) throws Exception {
		if (dir.exists()) {
			if (!dir.isDirectory()) {
				throw new Exception(dir + " is not a directory");
			}
			return false;
		} else {
			if (!dir.mkdirs()) {
				throw new Exception("Could not create " + dir);
			}
			return true;
		}
	}

	private void convert(String[] args) throws Exception {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-o")) {
				i++;
				this.output = new File(args[i]);
				ensureDir(this.output);
				continue;
			}

			File f = new File(args[i]);
			addInputFile(f);
		}

		for (File f : this.inputs) {
			Java2PythonVisitor v = new Java2PythonVisitor();
			CompilationUnit root = JavaParser.parse(f);
			root.accept(v, root);

			String python = v.getSource();
			if (this.output == null) {
				System.out.println(python);
				continue;
			}
			String pkg = v.getPackage();
			pkg = pythonizePackage(pkg);

			pkg = pkg.replaceAll("\\.", "/");
			File pkgDir = new File(this.output, pkg);
			ensureDir(pkgDir);
			File init = new File(pkgDir, "__init__.py");
			if (!init.exists()) {
				PrintWriter writer = new PrintWriter(init);
				writer.println();
				writer.close();
			}

			String module = f.getName();
			module = module.substring(0, module.length() - 5);

			File newFile = new File(pkgDir, module + ".py");
			PrintWriter writer = new PrintWriter(newFile);
			writer.println(python);
			writer.close();
		}

	}

	public static String pythonizePackage(String pkg) {
		if (pkg == null) {
			return "";
		}
		String[] pkgs =  pkg.split("\\.");
		if (pkgs.length > 1) {
			if (pkgs[0].equals("com") || pkgs[0].equals("org")) {
				return pkg.substring(pkg.indexOf(".") + 1);
			}
		}

		return pkg;
	}

	public static void main(String[] args) throws Exception {
		Main converter = new Main();
		converter.convert(args);

	}

}
