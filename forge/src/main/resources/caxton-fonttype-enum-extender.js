// Thanks to the random souls who wrote these guides:
// https://cadiboo.github.io/tutorials/1.14.4/forge/99.99-coremod/
// https://linuxtut.com/how-to-write-a-core-mod-in-minecraft-forge-1.15.2-de7fb/

var ASMAPI = Java.type("net.minecraftforge.coremod.api.ASMAPI");
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type("org.objectweb.asm.tree.InsnList");
var AbstractInsnNode = Java.type("org.objectweb.asm.tree.AbstractInsnNode");
var FieldInsnNode = Java.type("org.objectweb.asm.tree.FieldInsnNode");
var MethodInsnNode = Java.type("org.objectweb.asm.tree.MethodInsnNode");
var VarInsnNode = Java.type("org.objectweb.asm.tree.VarInsnNode");
var TypeInsnNode = Java.type("org.objectweb.asm.tree.TypeInsnNode");
var LdcInsnNode = Java.type("org.objectweb.asm.tree.LdcInsnNode");
var InsnNode = Java.type("org.objectweb.asm.tree.InsnNode");
var FieldNode = Java.type("org.objectweb.asm.tree.FieldNode");

// TODO: we probably need to use SRG names in prod

// In a prod env, this will be mapped to the SRG name (m_135820_), while
// in a dev env, it will be mapped to the Yarn name (tryParse).
var identifierTryParse = ASMAPI.mapMethod("m_135820_");
var isDevEnv = identifierTryParse != "m_135820_";

// Coremod doesn’t support remapped classes directly (https://github.com/MinecraftForge/CoreMods/issues/25).
// Branch on isDevEnv instead.
var FONT_TYPE_CLASS = isDevEnv ? 'net.minecraft.client.font.FontType' : 'net.minecraft.client.gui.font.providers.GlyphProviderType';
var FONT_TYPE_CLASS_INTL = isDevEnv ? 'net/minecraft/client/font/FontType' : 'net/minecraft/client/gui/font/providers/GlyphProviderType';
var FONT_TYPE_DESC = 'L' + FONT_TYPE_CLASS_INTL + ';';
// For methods, we can use the ASMAPI.mapMethod function directly.
var VALUES_FACTORY_METHOD = ASMAPI.mapMethod("m_285865_"); // method_36876

function initializeCoreMod() {
    return {
        'Font type enum extender': {
            'target': {
                'type': 'CLASS',
                'name': FONT_TYPE_CLASS,
            },
            'transformer': transformFontType,
        },
        'Font type values factory transformer': {
            'target': {
                'type': 'METHOD',
                'class': FONT_TYPE_CLASS,
                'methodName': VALUES_FACTORY_METHOD,
                'methodDesc': '()[' + FONT_TYPE_DESC,
            },
            'transformer': transformValuesFactory,
        },
        'Static initializer transformer': {
            'target': {
                'type': 'METHOD',
                'class': FONT_TYPE_CLASS,
                'methodName': '<clinit>',
                'methodDesc': '()V',
            },
            'transformer': transformStaticInitializer,
        },
    }
}

function transformFontType(cls) {
    print("iaj zav; xbsio la, n v km l s zioa");
    cls.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "CAXTON", FONT_TYPE_DESC, null, null));
    return cls;
}

function transformValuesFactory(method) {
    var n = method.maxLocals;
    method.maxLocals = n + 1;
    var i;
    var insnList;
    var instrs = method.instructions;
    // Increase the allocated size of the array by 1
    var newArrayInstr = null;
    for (i = 0; i < instrs.size(); ++i) {
        var instr = instrs.get(i);
        if (instr.getOpcode() == Opcodes.ANEWARRAY) {
            newArrayInstr = instr;
            break;
        }
    }
    if (newArrayInstr == null) {
        print("Failed to detect any ANEWARRAY instruction.");
        return;
    }
    insnList = new InsnList();
    // Save the old length so we can use it as an index
    insnList.add(new InsnNode(Opcodes.DUP));
    insnList.add(new VarInsnNode(Opcodes.ISTORE, n));
    insnList.add(new InsnNode(Opcodes.ICONST_1));
    insnList.add(new InsnNode(Opcodes.IADD));
    instrs.insertBefore(newArrayInstr, insnList);

    // Insert our variant at the last index
    var returnInstr = null;
    for (i = 0; i < instrs.size(); ++i) {
        var instr = instrs.get(i);
        if (instr.getOpcode() == Opcodes.ARETURN) {
            returnInstr = instr;
            break;
        }
    }
    if (returnInstr == null) {
        print("Failed to detect any ARETURN instruction.");
        return;
    }
    insnList = new InsnList();
    insnList.add(new InsnNode(Opcodes.DUP));
    insnList.add(new VarInsnNode(Opcodes.ILOAD, n));
    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, FONT_TYPE_CLASS_INTL, "CAXTON", FONT_TYPE_DESC));
    insnList.add(new InsnNode(Opcodes.AASTORE));
    instrs.insertBefore(returnInstr, insnList);
    return method;
}

// Go Identifier yourself, LexManos!
function transformStaticInitializer(method) {
    var n = method.maxLocals;
    method.maxLocals = n + 1;
    var i;
    var insnList;
    var instrs = method.instructions;
    // Search for the call to the values factory.
    var vfMethodCall;
    for (i = 0; i < instrs.size(); ++i) {
        var instr = instrs.get(i);
        if (instr.getOpcode() == Opcodes.INVOKESTATIC && instr.name == VALUES_FACTORY_METHOD) {
            vfMethodCall = instr;
            break;
        }
    }
    if (vfMethodCall == null) {
        print("Failed to detect any calls to the values factory.");
        return;
    }
    // Now we inject right after the latest preceding PUTSTATIC
    var lastEnumVariantStore = vfMethodCall;
    while (lastEnumVariantStore.getOpcode() != Opcodes.PUTSTATIC) {
        lastEnumVariantStore = lastEnumVariantStore.getPrevious();
        if (lastEnumVariantStore == null) {
            print("Failed to detect the store to the last enum variant.");
            return;
        }
    }
    // First, insert a DUP right before the store:
    instrs.insertBefore(lastEnumVariantStore, new InsnNode(Opcodes.DUP));
    // Now create our own variant
    insnList = new InsnList();
    // Get the ordinal of the last variant that was constructed.
    // The ordinal of the next variant will be one more than that.
    insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Enum", "ordinal", "()I"));
    insnList.add(new InsnNode(Opcodes.ICONST_1));
    insnList.add(new InsnNode(Opcodes.IADD));
    // <ordinal>
    // Create a new instance of FontType
    insnList.add(new TypeInsnNode(Opcodes.NEW, FONT_TYPE_CLASS_INTL));
    // <ordinal> <font type>
    insnList.add(new InsnNode(Opcodes.DUP_X1));
    // <font type> <ordinal> <font type>
    insnList.add(new InsnNode(Opcodes.SWAP));
    // <font type> <font type> <ordinal>
    insnList.add(new LdcInsnNode("CAXTON"));
    insnList.add(new InsnNode(Opcodes.SWAP));
    // <font type> <font type> <name> <ordinal>
    insnList.add(new LdcInsnNode("caxton"));
    // <font type> <font type> <name> <ordinal> <id>
    insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, "xyz/flirora/caxton/font/CaxtonFontLoader", "CODEC", "Lcom/mojang/serialization/MapCodec;"));
    // <font type> <font type> <name> <ordinal> <id> <loader codec>
    insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, FONT_TYPE_CLASS_INTL, "<init>", "(Ljava/lang/String;ILjava/lang/String;Lcom/mojang/serialization/MapCodec;)V"));
    // <font type>
    insnList.add(new FieldInsnNode(Opcodes.PUTSTATIC, FONT_TYPE_CLASS_INTL, "CAXTON", FONT_TYPE_DESC));
    instrs.insert(lastEnumVariantStore, insnList);
    return method;
}
