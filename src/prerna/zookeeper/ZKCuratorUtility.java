package prerna.zookeeper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreV2;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;

public final class ZKCuratorUtility {

	private CuratorFramework curator; 
	
	/**
	 * 
	 * @param curator
	 */
	ZKCuratorUtility(CuratorFramework curator) {
		this.curator = curator;
	}
	
	/**
	 * 
	 * @param pathToCheck
	 * @throws Exception
	 */
	public void createPathIfNotExists(String pathToCheck) throws Exception {
        if (this.curator.checkExists().forPath(pathToCheck) == null) {
        	this.curator.create().creatingParentsIfNeeded().forPath(pathToCheck, new byte[0]);
        }
	}
	
	/**
	 * 
	 * @param prefix
	 * @return
	 * @throws Exception
	 */
	public String createSequentialPersistentNode(String prefix) throws Exception {
        return this.curator.create()
        	.creatingParentsIfNeeded()
        	.withMode(CreateMode.PERSISTENT_SEQUENTIAL)
        	.forPath(prefix, new byte[0]);
	}
	
	/**
	 * 
	 * @param path
	 * @param data
	 * @throws Exception
	 */
	public void createEphemeralNode(String path, byte[] data) throws Exception {
		this.curator.create()
			.creatingParentsIfNeeded()
			.withMode(CreateMode.EPHEMERAL)
			.forPath(path, data);
	}
	
	/**
	 * 
	 * @param path
	 * @param data
	 * @throws Exception
	 */
	public void createEphemeralSequentialNode(String path, byte[] data) throws Exception {
		this.curator.create()
			.creatingParentsIfNeeded()
			.withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
			.forPath(path, data);
	}
	
	/**
	 * Create a znode with a specified path, mode and data
	 * 
	 * @param path
	 * @param mode
	 * @throws Exception
	 */
	public void createNode(String path, CreateMode mode, byte[] data) throws Exception {
		this.curator.create()
			.creatingParentsIfNeeded()
			.withMode(mode)
			.forPath(path, data);
	}
	
	
	/**
	 * This method returns of list a list of children nodes based on the 
	 * path passed in. 
	 * 
	 * @param path
	 * @throws Exception
	 */
	public List<String> listNodeChildren(String path) throws Exception {
		return this.curator.getChildren().forPath(path);
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public byte[] getDataFromNode(String path) throws Exception {
		 Stat stat = new Stat();
		 return this.curator.getData().storingStatIn(stat).forPath(path);
	}
	
	/**
	 * 
	 * @param parentZNode
	 * @return
	 * @throws Exception
	 */
	public String getLatestZNode(String parentZNode) throws Exception {
		// if the parent node doesn't exist
		// then return null
        if(this.curator.checkExists().forPath(parentZNode) == null) {
        	return null;
        }
        
        // Get the list of znodes under the parent path
        Stat latestStat = null;
        String latestZNode = null;

        for (String child : this.curator.getChildren().forPath(parentZNode)) {
            String childPath = parentZNode + "/" + child;
            Stat childStat = new Stat();
            this.curator.getData().storingStatIn(childStat).forPath(childPath);

            if (latestStat == null || childStat.getCtime() > latestStat.getCtime()) {
                latestStat = childStat;
                latestZNode = childPath;
            }
        }
        
        return latestZNode;
	}
	
	/**
	 * 
	 * @param parentZNode
	 * @throws Exception
	 */
	public void deleteAllZNodeChildren(String parentZNode) throws Exception {
        List<String> children = this.curator.getChildren().forPath(parentZNode);
        
        // Delete all children
        for (String child : children) {
            String childPath = parentZNode + "/" + child;
            this.curator.delete().forPath(childPath);
        }
	}
	
	/**
	 * 
	 * @param path
	 * @throws Exception 
	 */
	public void deletePath(String path) throws Exception {
		this.curator.delete().forPath(path);
	}
	
	/**
	 * This method removes a node and all of it's children from the Zookeeper server
	 * 
	 * @param path - The path of the node to be deleted
	 * @throws Exception
	 */
	public void deleteRecursively(String path) throws Exception {
		if (this.curator.checkExists().forPath(path) != null) {
	        this.curator.delete().deletingChildrenIfNeeded().forPath(path);
		}
    }
	
	/**
	 * 
	 * @param path
	 * @param watcher
	 * @throws Exception
	 */
	public void setWatcherForPath(String path, Watcher watcher) throws Exception {
		Stat stat = curator.checkExists().usingWatcher(watcher).forPath(path);
		if(stat == null) {
			throw new IllegalArgumentException("ZNode path " + path + " does not exist");
		}
	}
	
	/**
	 * 
	 * @param lockName
	 * @return
	 */
	public InterProcessMutex getLock(String lockName) {
		return new InterProcessMutex(this.curator, lockName);
	}
	
	/**
	 * 
	 * @param lockName
	 * @param maxNumLeases
	 * @return
	 */
	public InterProcessSemaphoreV2 getTimeBasedLock(String lockName, int maxNumLeases) {
		return new InterProcessSemaphoreV2(this.curator, lockName, maxNumLeases);
	}
	
	/**
	 * 
	 * @param lockName
	 * @return
	 * @throws Exception 
	 */
	public boolean lockHeld(String lockName) throws Exception {
		return curator.checkExists().forPath(lockName) != null;
	}
	
	
	/**
	 * This method attemps to acquire a lock on znode. If successful it will return
	 * a {@code InterProcessMutex} object, otherwise it will return null.
	 * 
	 * @param path - The path of the node to create the lock for
	 * @throws Exception
	 */
	public InterProcessMutex attemptLock(String lockPath) throws Exception {
	    InterProcessMutex lock = new InterProcessMutex(curator, lockPath);
	    
	    // Try to acquire the lock immediately, without waiting
	    if (lock.acquire(0, TimeUnit.MILLISECONDS)) {
	        // Successfully acquired the lock
	        return lock;
	    }
	    
	    // Lock is already held by another process
	    return null;
	}
}
