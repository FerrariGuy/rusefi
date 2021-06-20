package com.rusefi.newparse.layout;

import com.rusefi.newparse.parsing.*;

import java.io.PrintStream;

public class ArrayLayout extends Layout {
    protected final int length;

    protected final Layout prototypeLayout;

    public ArrayLayout(PrototypeField prototype, int length) {
        this.length = length;

        if (prototype instanceof ScalarField) {
            prototypeLayout = new ScalarLayout((ScalarField)prototype);
        } else if (prototype instanceof EnumField) {
            prototypeLayout = new EnumLayout((EnumField) prototype);
        } else if (prototype instanceof StringField) {
            prototypeLayout = new StringLayout((StringField) prototype);
        } else if (prototype instanceof StructField) {
            StructField structPrototype = (StructField)prototype;
            prototypeLayout = new StructLayout(0, prototype.name, structPrototype.struct);
        } else {
            throw new RuntimeException("unexpected field type during array layout");
        }
    }

    @Override
    public int getSize() {
        return this.prototypeLayout.getSize() * this.length;
    }

    @Override
    public int getAlignment() {
        // Arrays only need to be aligned on the alignment of the element, not the whole array
        return this.prototypeLayout.getAlignment();
    }

    @Override
    public void setOffset(int offset) {
        super.setOffset(offset);
        this.prototypeLayout.setOffset(offset);
    }

    @Override
    public void setOffsetWithinStruct(int offset) {
        super.setOffsetWithinStruct(offset);
        this.prototypeLayout.setOffsetWithinStruct(offset);
    }

    @Override
    public String toString() {
        return "Array of " + this.prototypeLayout.toString() + " length " + this.length + " " + super.toString();
    }

    @Override
    public void writeTunerstudioLayout(PrintStream ps, StructNamePrefixer prefixer) {
        this.prototypeLayout.writeTunerstudioLayout(ps, prefixer, this.length);
    }

    @Override
    public void writeCLayout(PrintStream ps) {
        // Skip zero length arrays, they may be used for padding
        if (this.length > 0) {
            this.prototypeLayout.writeCLayout(ps, this.length);
        }
    }
}