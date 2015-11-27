package com.tower.service.filter;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.tower.service.log.Logger;
import com.tower.service.log.LoggerFactory;
import com.tower.service.util.CacheSwitcher;
import com.tower.service.util.RequestID;

@Activate(group = { Constants.PROVIDER, Constants.CONSUMER })
public class DubboServiceFilter implements Filter {

	private Logger logger = LoggerFactory.getLogger(DubboServiceFilter.class);
	
	private String reqidKey = "TowerReqId";
	private String cachedKey = "TowerCached";

	public DubboServiceFilter() {
		logger.info("DubboServiceFilter created");
	}

	@Override
	public Result invoke(Invoker<?> invoker, Invocation invocation)
			throws RpcException {
		RpcContext context = RpcContext.getContext();
		String remoteIp = context.getRemoteAddressString();
		boolean provider = context.isProviderSide();
		URL url = invoker.getUrl();
		String interfaceStr = url.getParameter("interface");
		if (provider) {
			String reqId = invocation.getAttachment(reqidKey);
			RequestID.set(reqId);
			boolean cached = Boolean.valueOf(invocation.getAttachment(cachedKey,"true"));
			CacheSwitcher.set(cached);
		} else {
			String reqId = RequestID.get();
			invocation.getAttachments().put(reqidKey, reqId);
			Boolean cached = CacheSwitcher.get();
			invocation.getAttachments().put(cachedKey, cached==null?null:String.valueOf(cached));
		}
		long start = System.currentTimeMillis();
		Result result =null;
		try{
			result = invoker.invoke(invocation);
		}finally{
			long timeused = (System.currentTimeMillis() - start);
			if(!"com.alibaba.dubbo.monitor.MonitorService".equalsIgnoreCase(interfaceStr)){
				logger.info("{} {} interface={}.{} timeused={}",provider?"from client":"to service",remoteIp,interfaceStr,invocation.getMethodName(),timeused);
			}
		}
		return result; 
	}
}