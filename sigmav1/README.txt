SIGMA ADDON LEAKED - DEVELOPER SOURCE
=====================================

Target:
  Minecraft 1.21.11
  Java 21
  Fabric Loader 0.18.2
  Meteor Client 1.21.11

This project contains all 23 recovered top-level Java source files, including
the inner classes embedded in those files, plus every non-class resource from
the final JAR. Minecraft intermediary symbols were translated to Yarn names.

The original custom classes were already plainly named and were not protected
by an obfuscator. Normal Java lambda and string-concatenation invokedynamic
sites were preserved instead of being mistaken for encrypted string factories.

Static safety result:
  No malware, RAT, stealer, webhook, hardcoded IP, token logger, native payload,
  process execution, external file access, auth/license/HWID gate, or honeypot
  was found. The StaffList username comparison is ordinary staff detection,
  not authentication.

The target JAR was never executed or class-loaded. The source was produced with
static bytecode reading and trusted decompiler/mapping tools only.
