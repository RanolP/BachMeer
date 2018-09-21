package io.github.ranolp.bachmeer

import io.github.ranolp.bachmeer.compile.BatchCompiler
import io.github.ranolp.bachmeer.compile.CompilerOption

class BatchTest : CompilerTest(BatchCompiler(CompilerOption(useMagicLookup = true)))
