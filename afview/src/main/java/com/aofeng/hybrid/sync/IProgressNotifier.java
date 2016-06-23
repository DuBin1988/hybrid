package com.aofeng.hybrid.sync;

public interface IProgressNotifier {
	public void notifyProgress(int progress);
	public void notifyDone(boolean done);
	public void prelude();
}
