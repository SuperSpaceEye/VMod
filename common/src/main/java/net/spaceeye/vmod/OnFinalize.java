package net.spaceeye.vmod;

public abstract class OnFinalize {
    protected abstract void onFinalize();

    protected void finalize() throws Throwable {
        onFinalize();
        super.finalize();
    }
}
