/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*
* @authors Andrew Dinn
*/
package org.jboss.jbossts.orchestration.agent.adapter;

import org.objectweb.asm.*;
import org.jboss.jbossts.orchestration.rule.type.TypeHelper;
import org.jboss.jbossts.orchestration.rule.Rule;

import java.util.List;
import java.util.ArrayList;

/**
 * asm Adapter class used to check that the target method for a rule exists in a class
 */
public class ThrowCheckAdapter extends RuleCheckAdapter
{
     public ThrowCheckAdapter(ClassVisitor cv, Rule rule, String targetClass, String targetMethod, String exceptionClass, int count)
    {
        super(cv, rule, targetClass, targetMethod);
        this.exceptionClass = exceptionClass;
        this.count = count;
        this.visitedCount = 0;
    }

    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (matchTargetMethod(name, desc)) {
            return new ThrowCheckMethodAdapter(mv, rule, access, name, desc, signature, exceptions);
        }

        return mv;
    }

    /**
     * a method visitor used to add a rule event trigger call to a method
     */

    private class ThrowCheckMethodAdapter extends RuleCheckMethodAdapter
    {
        private int access;
        private String name;
        private String descriptor;
        private String signature;
        private String[] exceptions;
        private boolean visited;

        ThrowCheckMethodAdapter(MethodVisitor mv, Rule rule, int access, String name, String descriptor, String signature, String[] exceptions)
        {
            super(mv, rule, access, name, descriptor);
            this.access = access;
            this.name = name;
            this.descriptor = descriptor;
            this.signature = signature;
            this.exceptions = exceptions;
            visitedCount = 0;
        }

        public void visitInsn(final int opcode) {
            if (opcode == Opcodes.ATHROW) {
                // ok, we have hit a throw -- for now we just count any throw
                // later we will try to match the exception class
                if (visitedCount < count) {
                    visitedCount++;
                    if (visitedCount == count) {
                        // and we have enough occurences to match the count
                        setTriggerPoint();
                    }
                }
            }

            super.visitInsn(opcode);
        }

        public void visitEnd()
        {
            if (checkBindings()) {
                setVisitOk();
            }
            super.visitEnd();
        }
    }

    private String exceptionClass;
    private int count;
    private int visitedCount;
}