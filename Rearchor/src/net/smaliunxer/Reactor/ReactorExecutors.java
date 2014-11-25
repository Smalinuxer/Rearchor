package net.smaliunxer.Reactor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * not finish yet
 */
public class ReactorExecutors<T>{
	
	private ExecutorService handleReactor = Executors.newCachedThreadPool();
	
	protected boolean status = false;
	
	private static final int DEFUALT_WAIT_TIMEOUT = 1000;
	
	private Lock lock;
	
	protected Semaphore mSemaphore;
	
	private static final int MAX_REACT_NUM = 10;
	
	public ReactorExecutors(){
		this.mSemaphore = new Semaphore(MAX_REACT_NUM);
		lock = new ReentrantLock();
	}
	
	public void shutdown(){
		lock.lock();	//��������sychronized��̫����
		if(status != false){
			status = false;
			lock.unlock();
			try {
				if(handleReactor.awaitTermination(DEFUALT_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)){
					handleReactor.shutdownNow();
				}
			} catch (InterruptedException e) {
				handleReactor.shutdownNow();
				//TODO ��־��¼
			}
			//TODO �ر�����
		}
	}
	
	public synchronized boolean isRunning(){
		return status;
	}
	
	/*
	 * 
	 * ͨ���ۺ������,�ź���Ӧ����runnable�н��п���?
	private Semaphore mSemaphore = null;
	
	public ReactorExecutors(){
		
	}
	*/
	public void reactInPool(Runnable commend) throws ReactorShutDownException{
		try {
			cherkStatus();
		} catch (ReactorShutDownException e) {
			throw e;
		}
		try {
			mSemaphore.acquire();
		} catch (InterruptedException e) {
			//������ϵ�ʱ������˳���ʱ��
		}
		handleReactor.execute(commend);
		mSemaphore.release();
	}
	
	/**
	 * @param commend
	 * @return This Future object can be used to check if the Runnable as finished executing.
	 * @throws ReactorShutDownException 
	 */
	public Future<?> reactInPoolforResult(Runnable commend) throws ReactorShutDownException{
		try {
			cherkStatus();
		} catch (ReactorShutDownException e) {
			throw e;
		}
		try {
			mSemaphore.acquire();
		} catch (InterruptedException e) {
			//TODO ������ϵ�ʱ������˳���ʱ��
		}
		Future<?> future = handleReactor.submit(commend);
		mSemaphore.release();
		return future;
	}
	
	public Future<T> reactInPoolforResult(Callable<T> commend) throws ReactorShutDownException{
		try {
			cherkStatus();
		} catch (ReactorShutDownException e) {
			throw e;
		}
		try {
			mSemaphore.acquire();
		} catch (InterruptedException e) {
			//TODO ������ϵ�ʱ������˳���ʱ��
		}
		Future<T> future = handleReactor.submit(commend);
		mSemaphore.release();
		return future;
	}
	
	@Deprecated
	public List<Future<T>> reactInPoolforResult(Collection<? extends Callable<T>> commends) throws ReactorShutDownException{
		try {
			cherkStatus();
		} catch (ReactorShutDownException e) {
			throw e;
		}
		
		try {
			return handleReactor.invokeAll(commends);
		} catch (InterruptedException e) {
			//TODO LOG
			shutdown();
			return null;
		}
	}
	
	protected void cherkStatus() throws ReactorShutDownException{
		if(!isRunning()){
			throw new ReactorShutDownException();
		}
	}
}
