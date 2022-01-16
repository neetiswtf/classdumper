package ru.neetis.classdumper;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

public class Main {

    /*
        This field value used for trying to restore possible class file.
        We are trying to restore classes that sizes is lower than this value

        size is in bytes btw
     */

    public static final long MAX_CLASS_SIZE = 25600; // aka 200 kb
    public static int restoredClasses;

    public static void main(final String[] args) throws IOException {
        System.out.println("super duper class dumper 3000");
        if(args.length < 1){
            System.out.println("Invalid usage bro");
            System.exit(0);
        }

        /*
            Max dump file size is 2 GB.
            You can't restore classes from dump that greater than 2gb
             /shrug
         */
        final byte[] bytes = Files.readAllBytes(new File(args[0]).toPath());
        final JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(args[0] + "_restored.jar"));

        for(int i = 0; i < bytes.length; ++i) {
            /*
                Java class file has own magic number. It's 0xCAFEBABE
                JVM using magic number to know are it's really class file?
             */
            if(i + 4 < bytes.length && (bytes[i] == (byte)0xCA && bytes[i + 1] == (byte)0xFE && bytes[i + 2] == (byte)0xBA && bytes[i + 3] == (byte)0xBE)){
                System.out.print("Trying to restore possible class file at " + i);
                final ClassNode classNode = restore(bytes, i);
                if(classNode == null){
                    System.out.println(": fail");
                }else{
                    System.out.println(": ok [" + classNode.name + "]");
                    ++restoredClasses;
                    try{
                        jarOutputStream.putNextEntry(new ZipEntry(classNode.name + ".class"));
                        final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                        classNode.accept(classWriter);
                        jarOutputStream.write(classWriter.toByteArray());
                        jarOutputStream.closeEntry();
                    }catch(final ZipException exception){
                        System.err.println("Cannot write " + classNode.name + " class (" + exception.getMessage() + ")");
                    }
                }
            }
        }

        System.out.println("Total restored classes: " + restoredClasses);
        jarOutputStream.flush();
        jarOutputStream.close();
    }

    private static ClassNode restore(final byte[] bytes, int index){
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        /*
            We are trying to read all dumped bytes before their size exceeds the maximum
         */
        while(byteArrayOutputStream.size() < MAX_CLASS_SIZE && index < bytes.length){
            byteArrayOutputStream.write(bytes[index]);
            try{
                final ClassReader classReader = new ClassReader(byteArrayOutputStream.toByteArray());
                final ClassNode classNode = new ClassNode();
                classReader.accept(classNode, Opcodes.ASM5);
                return classNode;
            }catch(final Exception exception) {
                ++index;
            }
        }

        return null; // Well, it's not a class :(
    }
}
