package sak.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate

class SakModuleProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("sak.sdk.bundle.SakModule")
        val objects = symbols.filter { it is KSClassDeclaration && it.classKind == ClassKind.OBJECT }
        
        if (!objects.iterator().hasNext()) return emptyList()

        val file = codeGenerator.createNewFile(
            Dependencies(false, *objects.map { it.containingFile!! }.toList().toTypedArray()),
            "sak.sdk.bundle",
            "GeneratedSakModules"
        )

        val moduleNames = objects.map { (it as KSClassDeclaration).qualifiedName?.asString() ?: "" }
            .filter { it.isNotEmpty() }

        file.write("package sak.sdk.bundle\n\n".toByteArray())
        file.write("import sak.sdk.bundle.FeatureModule\n\n".toByteArray())
        file.write("val sakDiscoveredModules: List<FeatureModule> = listOf(\n".toByteArray())
        moduleNames.forEach { name ->
            file.write("    $name,\n".toByteArray())
        }
        file.write(")\n".toByteArray())
        file.close()

        return symbols.filter { !it.validate() }.toList()
    }
}

class SakModuleProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SakModuleProcessor(environment.codeGenerator, environment.logger)
    }
}
