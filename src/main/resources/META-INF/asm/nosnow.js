// noinspection ES6ConvertVarToLetConst

var ASM = Java.type('net.neoforged.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');

var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');

// noinspection JSUnusedGlobalSymbols
function initializeCoreMod() {
    return {
        'nosnow': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.world.level.biome.Biome',
                'methodName': 'shouldSnow',
                'methodDesc': '(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z'
            },
            'transformer': function (/*org.objectweb.asm.tree.MethodNode*/ methodNode) {
                var /*org.objectweb.asm.tree.InsnList*/ instructions = methodNode.instructions;
                instructions.insert(
                    ASM.findFirstInstruction(methodNode, Opcodes.ICONST_1),
                    ASM.listOf(
                        new VarInsnNode(Opcodes.ALOAD, 0),
                        new VarInsnNode(Opcodes.ALOAD, 1),
                        new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            'tamaized/voidscape/asm/ASMHooks',
                            'shouldSnow',
                            '(ZLnet/minecraft/world/level/biome/Biome;Lnet/minecraft/world/level/LevelReader;)Z',
                            false
                            )
                        )
                    );
                return methodNode;
            }
        },
        'noice': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.world.level.biome.Biome',
                'methodName': 'shouldFreeze',
                'methodDesc': '(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z'
            },
            'transformer': function (/*org.objectweb.asm.tree.MethodNode*/ methodNode) {
                var /*org.objectweb.asm.tree.InsnList*/ instructions = methodNode.instructions;
                var inst = [];
                for (var index = 0; index < instructions.size(); index++) {
                    var /*org.objectweb.asm.tree.MethodInsnNode*/ node = instructions.get(index);
                    if (node.getOpcode() === Opcodes.ICONST_1)
                        inst.push(node);
                }
                inst.forEach(function (/*org.objectweb.asm.tree.AbstractInsnNode*/ value, index, array) {
                    instructions.insert(
                        value,
                        ASM.listOf(
                            new VarInsnNode(Opcodes.ALOAD, 0),
                            new VarInsnNode(Opcodes.ALOAD, 1),
                            new MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                'tamaized/voidscape/asm/ASMHooks',
                                'shouldSnow',
                                '(ZLnet/minecraft/world/level/biome/Biome;Lnet/minecraft/world/level/LevelReader;)Z',
                                false
                                )
                            )
                        )
                });
                return methodNode;
            }
        }
    }
}
