package com.dooapp.gaedo.informer;

import japa.parser.ASTHelper;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.JavadocComment;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.type.ClassOrInterfaceType;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.dooapp.gaedo.finders.Informer;
import com.dooapp.gaedo.finders.informers.ObjectFieldInformer;
import com.dooapp.gaedo.finders.root.BasicFieldInformerLocator;
import com.dooapp.gaedo.utils.Utils;

/**
 * Generates the text of the informer generator
 * 
 * @author ndx
 * 
 */
public class InformerTextGenerator {

	public static CompilationUnit generateCompilationUnit(InformerInfos informerInfos, Collection<String> qualifiedEnums, Map<String, Class> resolvedInformers) {
		CompilationUnit cu = new CompilationUnit();
		// set the package
		cu.setPackage(new PackageDeclaration(ASTHelper.createNameExpr(informerInfos.classPackage)));

		// Finally add imports (they may have been "improved" by informers generation)
		List<ImportDeclaration> baseImports = informerInfos.imports;
		// Extracting effective imports
		Collection<String> imports = new LinkedList<String>();
		
		// Add current package to import for resolution (but do not forget to remove it before writing the effective imports)
		imports.add(informerInfos.classPackage);
		imports.add("java.lang");
		for (ImportDeclaration d : baseImports) {
			imports.add(d.getName().toString());
		}

		// create the type declaration
		ClassOrInterfaceDeclaration type = new ClassOrInterfaceDeclaration(ModifierSet.PUBLIC, true, informerInfos.getInformerName());
		type.setExtends(Arrays.asList(new ClassOrInterfaceType(Informer.class.getSimpleName() + "<" + informerInfos.className + ">")));
		type.setJavaDoc(new JavadocComment("\n" + "Informer for {@link " + informerInfos.className + "}\n" + "@author InformerMojos\n"));
		ASTHelper.addTypeDeclaration(cu, type);
		for (PropertyInfos infos : informerInfos.properties) {
			// create a method
			MethodDeclaration method = new MethodDeclaration(ModifierSet.PUBLIC, ASTHelper.VOID_TYPE, "get" + Utils.uppercaseFirst(infos.name));
			String informerTypeFor = InformerTypeFinder.getInformerTypeFor(resolvedInformers, qualifiedEnums, imports, infos);
			method.setType(new ClassOrInterfaceType(informerTypeFor));
			method.setJavaDoc(new JavadocComment(infos.generateJavadoc(informerInfos, informerTypeFor)));
			ASTHelper.addMember(type, method);
		}
		
		imports.remove(informerInfos.classPackage);
		imports.remove("java.lang");

		baseImports = new LinkedList<ImportDeclaration>();
		for(String name : imports) {
			baseImports.add(new ImportDeclaration(ASTHelper.createNameExpr(name), false, false));
		}
		baseImports.add(new ImportDeclaration(ASTHelper.createNameExpr(Informer.class.getCanonicalName()), false, false));
		baseImports.add(new ImportDeclaration(ASTHelper.createNameExpr(ObjectFieldInformer.class.getPackage().getName()), false, true));
		cu.setImports(baseImports);
		return cu;
	}

}
