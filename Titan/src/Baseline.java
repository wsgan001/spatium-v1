import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.thinkaurelius.titan.core.TitanGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;

public class Baseline {
	
	public static Database db;
	public static TitanGraph graph;
	public static HashMap<String, Long> total_count; 
	public static double PI_threshold;
	public static boolean verbose;
	public static ConcurrentHashMap<List<String>,Float>  colocations;
	
	public Baseline(){
		this.db = Database.getInstance();
		this.graph = this.db.getTitanGraph();
		this.total_count = new HashMap<String, Long>();
		this.PI_threshold = 0.1;
		this.verbose = false;
		this.colocations = new ConcurrentHashMap<List<String>,Float>();
	}
	
	public Baseline(Database db, TitanGraph graph){
		this.db = db;
		this.graph = graph;
		this.total_count = new HashMap<String, Long>();
		this.PI_threshold = 0.001;
		this.verbose = false;
		this.colocations = new ConcurrentHashMap<List<String>,Float>();
	}
	
	public static boolean areConnected(long id1, long id2) 
	{	
		Vertex vertex1 = graph.getVertex(id1);
		String type1 = vertex1.getProperty("type");
		Vertex vertex2 = graph.getVertex(id2);
		String type2 = vertex2.getProperty("type");
		if(Integer.parseInt(type1) < Integer.parseInt(type2)){
			for(Iterator<Vertex> it = vertex1.getVertices(Direction.IN,type1+"-"+type2).iterator();
					it.hasNext();){
				Vertex vertex3 = it.next();
				if(id2 == ((long)vertex3.getId())){					
					return true;
				}
			}
		}
		if(Integer.parseInt(type1) > Integer.parseInt(type2))
		{			
			for(Iterator<Vertex> it = vertex2.getVertices(Direction.IN,type2+"-"+type1).iterator();
					it.hasNext();){
				Vertex vertex3 = it.next();
				if(id1 == ((long)vertex3.getId())){					
					return true;
				}
			}
		}
		return false;
	}

	public static boolean areConnected(long id1, final long id2, String label) {
		
		GremlinPipeline pipe = new GremlinPipeline();
		pipe.start(graph.getVertex(id1)).in(label).filter(new PipeFunction<Vertex, Boolean>() {
			public Boolean compute(Vertex argument){
				if((Long)argument.getId() == id2){
					return true;
				}
				else{
					return false;
				}
			}
		}).path(new PipeFunction<Vertex, Long>(){
			public Long compute(Vertex argument) {
				return (Long) argument.getId();
			}
		}).enablePath();
				
		if(pipe.hasNext()){
			return true;
		}else{
			return false;
		}
		
	}
	
	public static HashSet<List<String>> L1(){
		/*
		 * Generate colocations of size 1
		 * Iterate over all vertices of the graph
		 */
		System.out.println("Generating colocations of size 1\n");
		HashSet<List<String>> Ckplus1 = new HashSet<List<String>>();
		List<String> items = new ArrayList<String>();
				
		long time1 = System.currentTimeMillis();
		long counter = 0;

		for (Iterator<Vertex> iterator = graph.getVertices().iterator(); iterator.hasNext();) {
			Vertex vertex = iterator.next();
			String type = vertex.getProperty("type");
			if (total_count.containsKey(type)){
				total_count.put(type, total_count.get(type)+1);
			}
			else {
				total_count.put(type, (long) 1);
				items.add(type);
			}
			if(verbose){
				System.out.println(counter+" : "+"Id = "+vertex.getId()+" Place = "+vertex.getProperty("place")+" Type = "+vertex.getProperty("type")+" Visible = "+vertex.getProperty("visible"));
			}
			counter++;
		}
		long time2 = System.currentTimeMillis();
		System.out.println("Total number of colocations of size 1 = "+total_count.size());
		
		System.out.println("Time taken for size 1 : "+(time2-time1));
		for(int i = 0; i<items.size()-1;i++){
			for(int j = i+1; j<items.size();j++){
				List<String> temp_List = new ArrayList<String>();
				if (Integer.parseInt(items.get(i)) < Integer.parseInt(items.get(j))) {
					temp_List.add(items.get(i));
					temp_List.add(items.get(j));					
				}
				else{
					temp_List.add(items.get(j));
					temp_List.add(items.get(i));
				}
				Ckplus1.add(temp_List);
			}
		}
		return Ckplus1;
	}
	
	public static HashSet<List<String>> join_and_prune(HashMap<String, HashMap<String, Float>> Lk, int k){
		System.out.println("Joining and Pruning to get Candidate Colocations of Size "+(k+1));
		long time1 = System.currentTimeMillis();
		HashSet<List<String>> Ckplus1 = new HashSet<List<String>>();		
		Iterator it, it1, it2;
		it = Lk.entrySet().iterator();
		int candidate_count = 0;
		while(it.hasNext()){
			List<String> items = new ArrayList<String>();
			String[] itemskplus1 = new String[k+1];
			Map.Entry pair = (Map.Entry)it.next();
			String type1 = (String) pair.getKey();
//			System.out.println("Type = "+type1);
			
			for(int i =0; i<k-1; i++){
				itemskplus1[i] = type1.split(":")[i];
//				System.out.println(type1.split(":")[i]);
			}
			
			it1 = ((HashMap<String, Double>) pair.getValue()).entrySet().iterator();
			
			while(it1.hasNext()){
				
				Map.Entry pair1 = (Map.Entry)it1.next();
				String type2 = (String) pair1.getKey();
				items.add(type2);
//				System.out.println(type2);
			}
			
			for(int i=0;i<items.size()-1;i++){
				
				for(int j=i+1; j<items.size(); j++){
					if(Integer.parseInt(items.get(i)) > Integer.parseInt(items.get(j))){
						itemskplus1[k-1] = items.get(j);
						itemskplus1[k] = items.get(i);
					}
					else{
						itemskplus1[k-1] = items.get(i);
						itemskplus1[k] = items.get(j);
					}
					
//					for(int z = 0; z < k+1;z++){
//						System.out.print(itemskplus1[z]+",");
//					}
//					System.out.println("");
					
					
					boolean flag = true;
					for(int x = 0 ; x < k+1;x++){
						
						int counter = 0;
						String[] itemsk = new String[k];
						
						for(int y = 0; y < k+1;y++){
							if(y==x){
								continue;
							}
							else {
								itemsk[counter] = itemskplus1[y];
								counter++;
							}
						}
						
						String key = "", value = "";
						int y;
						for(y = 0 ; y < k-1;y++){
							key = key+itemsk[y]+":";
						}
						value = itemsk[y];
						key = key.substring(0, key.length()-1);
//						System.out.println(key+"---"+value);
					
						if(Lk.containsKey(key)){
							HashMap<String, Float> temp = Lk.get(key);
							if(temp.containsKey(value)){
								continue;
							}else{
								flag = false;
								break;
							}
						}
						else{
							flag = false;
							break;
						}						
					
					}
					
					if(flag){
						List<String> temp_List = new ArrayList<String>();
						for(int x = 0; x<k+1;x++){
							temp_List.add(itemskplus1[x]);
						}
						Ckplus1.add(temp_List);
						candidate_count++;
					}					
				}
			}
			
		}
		long time2 = System.currentTimeMillis();
//		System.out.println(time1+" ---- "+time2);
		System.out.println("Total time required for joining and pruning for candidate colocations of size "+(k+1)+" is "+(time2-time1));
		System.out.println("Total Candidates of size "+(k+1)+" are "+candidate_count);
		return Ckplus1;
	}

	public static void print_Frequent(HashMap<String, HashMap<String, Float>> Lk, int k){
		
		System.out.println("Frequent Colocations of Size "+k+" with their participation index");
		int counter = 0;
		Iterator it,it1;
		it = Lk.entrySet().iterator();
		while(it.hasNext()){
			
			Map.Entry pair = (Map.Entry)it.next();
			String type1 = (String) pair.getKey();
			it1 = ((HashMap<String, Double>) pair.getValue()).entrySet().iterator();
			
			while(it1.hasNext()){
				
				Map.Entry pair1 = (Map.Entry)it1.next();
				String type2 = (String) pair1.getKey();
				float pi = (Float) pair1.getValue();
//				System.out.println(type1+":"+type2+" = "+pi);
				counter++;
			}
		}
		System.out.println();
		System.out.println("Total frequent colocations of size "+k+" are "+counter);
	}
	
	public static HashMap<String, HashMap<String, Float>> L2(HashSet<List<String>> Ck){
		/*
		 * Generate colocations of size 2
		 * Iterate over all edges of the graph
		 */
		
		HashMap<String, HashMap<Long, Boolean>> global_count = new HashMap<String, HashMap<Long, Boolean>>();
		HashMap<String, HashMap<String, Float>> L2 = new HashMap<String, HashMap<String,Float>>();
		
		long time1 = System.currentTimeMillis();
		
		if(verbose){
			System.out.println("Generating colocations of size 2.\n");
		
		}
		int counter = 0;
		//could lead to buffer-overflow
		
//		GremlinPipeline pipe = new GremlinPipeline();
		
		Iterator it;
		it = Ck.iterator();
		String candidate, type1, type2;
		while(it.hasNext()){
			List<String> tempList = ((List<String>)it.next());
			type1 = tempList.get(0);
			type2 = tempList.get(1);
			candidate = type1+":"+type2;
			
//			System.out.println("Candidate being screwed right now is : "+candidate);
			
			GremlinPipeline pipe = new GremlinPipeline();
			
			pipe.start(graph.getVertices("type",type1)).in(type1+"-"+type2).path(new PipeFunction<Vertex, Long>(){
				public Long compute(Vertex argument) {
					return (Long) argument.getId();
				}
			}).enablePath();
			
			
			
			if(global_count.containsKey(type1+":"+type2)==false){
				HashMap<Long, Boolean> default_hashmap = new HashMap<Long, Boolean>();
				global_count.put(type1+":"+type2, default_hashmap);
			}
			if(global_count.containsKey(type2+":"+type1)==false){
				HashMap<Long, Boolean> default_hashmap = new HashMap<Long, Boolean>();
				global_count.put(type2+":"+type1, default_hashmap);
			}
			
				
			Iterator pit = pipe.iterator();
			
			while(pit.hasNext()){
				
//				System.out.println(pit.next().get(0).getClass());
				List<Long> tempList1 = ((List<Long>)pit.next());
				Long Id1 = tempList1.get(0);
				Long Id2 = tempList1.get(1);
//				//can be made cleaner if need be...but its java in the end so how much cleaner could it be :P
				if(global_count.get(type1+":"+type2).containsKey(Id1) == false ){
					global_count.get(type1+":"+type2).put(Id1, true);
				}
				if(global_count.get(type2+":"+type1).containsKey(Id2) == false ){
					global_count.get(type2+":"+type1).put(Id2, true);
				}
			}
			
			Double x1 = (double) global_count.get(type1+":"+type2).size();
			Double x2 = (double) global_count.get(type2+":"+type1).size();
			Double a = x1/total_count.get(type1);
			Double b = x2/total_count.get(type2);
			float PI = (float) java.lang.Math.min(a, b);

			if (verbose)
			{
				System.out.println(type1);
				System.out.println(type2);
				System.out.println(x1+" / "+total_count.get(type1)+" "+type1);
				System.out.println(x2+" / "+total_count.get(type2)+" "+type2);
				System.out.println(PI);
				System.out.println("\n\n");
			}
			if(PI >= PI_threshold)
			{
				if(verbose){
					System.out.println(type1+":"+type2);
					System.out.println(PI);
					System.out.println("\n");
				}
				counter+=1;
				
				if(L2.containsKey(type1)==false){
					HashMap<String, Float> tempHashMap = new HashMap<String, Float>();
					tempHashMap.put(type2, PI);
					L2.put(type1, tempHashMap);
				}
				else{
					L2.get(type1).put(type2, PI);
				}					
			}
			
			
		}		
				
		if (verbose){
			System.out.println("Total no. of colocations of size 2  are = "+counter);
		}
		long time2 = System.currentTimeMillis();
		System.out.println("Time taken for size 2 : "+(time2-time1));
		return L2;
		
	}
		
	public static HashMap<String, HashMap<String, Float>> multithreaded_L2(HashSet<List<String>> Ck, int k){
		
		long time1 = System.currentTimeMillis();
		HashMap<String, HashMap<String, Float>> L2 = new HashMap<String, HashMap<String,Float>>();
		System.out.println("Generating Colocations of Size "+k);
		Iterator it = Ck.iterator();
		ExecutorService executorService = Executors.newFixedThreadPool(48);
		
		while(it.hasNext()){
			ValidateTraversal validateTraversal = new ValidateTraversal((List<String>) it.next(), k);
			executorService.execute(validateTraversal);
		}
		executorService.shutdown();
		while(!executorService.isTerminated()){
			;
		}
		System.out.println("All the threads terminated successfully");
		it = colocations.entrySet().iterator();
		
		while(it.hasNext()){
			Map.Entry<List<String>, Float> pairs = (Map.Entry<List<String>, Float>) it.next();
			List<String> tempList = (List<String>) pairs.getKey();
			String type1 = tempList.get(0);
			String type2 = tempList.get(1);
			
			if(L2.containsKey(type1)==false){
				HashMap<String, Float> tempHashMap = new HashMap<String, Float>();
				tempHashMap.put(type2, pairs.getValue());
				L2.put(type1, tempHashMap);
			}
			else{
				L2.get(type1).put(type2, pairs.getValue());
			}	
		}
		long time2 = System.currentTimeMillis();
		System.out.println("Time taken for size 2 : "+(time2-time1));
		colocations.clear();
		return L2;
	}
	
	public static HashMap<String, HashMap<String, Float>> L3(HashSet<List<String>> Ck){
		/*
		 * Generate colocations of size 2
		 * Iterate over all edges of the graph
		 */
		
		HashMap<String, HashMap<String, Float>> L_3 = new HashMap<String, HashMap<String,Float>>();
		
		long time1 = System.currentTimeMillis();
		
		if(verbose){
			System.out.println("Generating colocations of size 3.\n");
		
		}
		int counter = 0;
		//could lead to buffer-overflow
		
//		GremlinPipeline pipe = new GremlinPipeline();
		
		Iterator it;
		it = Ck.iterator();
		String candidate, type1, type2, type3;
		while(it.hasNext())
		{
			
			HashMap<String, HashSet<Long>> unique = new HashMap<String, HashSet<Long>>();
			List<String> tempList = ((List<String>)it.next());
			type1 = tempList.get(0);
			type2 = tempList.get(1);
			type3 = tempList.get(2);
			for(int i=0;i<tempList.size();i++)
			{
				String key = tempList.get(i);
				if(unique.containsKey(key)==false)
				{
					HashSet<Long> tempHashSet = new HashSet<Long>();
					unique.put(key, tempHashSet);
				}
			}
//			candidate = type1+":"+type2+":"+type3;
			
//			System.out.println("Candidate being screwed right now is : "+candidate);
			
			final List<Vertex> temp = new ArrayList<Vertex>();
			GremlinPipeline pipe = new GremlinPipeline();
			pipe.start(graph.getVertices("type",type1)).sideEffect(new PipeFunction<Vertex, Vertex>(){
				public Vertex compute(Vertex argument){
					if(temp.size()>0){
						temp.remove(temp.size()-1);
					}
					temp.add(argument);
					return argument;
				}
				}).in(type1+"-"+type2).in(type2+"-"+type3).out(type1+"-"+type3).filter(new PipeFunction<Vertex,Boolean>() {
				  public Boolean compute(Vertex argument) {
					  if(temp.contains(argument)){
						  return true;
					  }
					  else{
						  return false;
					  }				  
					  }
					}).path(new PipeFunction<Vertex, Long>(){
				public Long compute(Vertex argument) {
					return (Long) argument.getId();
				}
			}).enablePath();
			
			
			
				
			Iterator pit = pipe.iterator();
			
			while(pit.hasNext())
			{
//				System.out.println(pit.next().get(0).getClass());
				List<Long> tempList1 = ((List<Long>)pit.next());
				Long Id1 = tempList1.get(0);
				Long Id2 = tempList1.get(1);
				Long Id3 = tempList1.get(2);
				unique.get(type1).add(Id1);
				unique.get(type2).add(Id2);
				unique.get(type3).add(Id3);
			}
			
			float count_type1 = total_count.get(type1);
			float count_type2 = total_count.get(type2);
			float count_type3 = total_count.get(type3);
			float pr_type1 = ((float) unique.get(type1).size())/count_type1;
			float pr_type2 = ((float) unique.get(type2).size())/count_type2;
			float pr_type3 = ((float) unique.get(type3).size())/count_type3;
			float pi = pr_type1;
			if(pr_type2 < pi)
			{
				if(pr_type3 < pi)
				{
					pi = pr_type3;
				}
				else
				{
					pi = pr_type2;
				}
			}				
			else
			{
				if(pr_type3 < pi)
				{
					pi = pr_type3;
				}
			}
			if(pi>=PI_threshold)
			{
				if(verbose)
				{
					System.out.println("--------------");
					System.out.println("Frequent = "+type1+":"+type2+":"+type3+" PI = "+pi);
					System.out.println("Unique_Count = "+unique.get(type1).size()+":"+unique.get(type2).size()+":"+unique.get(type3).size());
					System.out.println("Total_Count = "+count_type1+":"+count_type2+":"+count_type3);
				}
				if(L_3.containsKey(type1+":"+type2)==false)
				{
					HashMap<String, Float> tempHashMap = new HashMap<String, Float>();
					tempHashMap.put(type3, pi);
					L_3.put(type1+":"+type2, tempHashMap);
				}
				else
				{
					L_3.get(type1+":"+type2).put(type3, pi);
				}
			}	
		}
		
		if (verbose)
		{
			System.out.println("Total no. of colocations of size 2  are = "+counter);
		}
		long time2 = System.currentTimeMillis();
		System.out.println("Time taken for size 2 : "+(time2-time1));
		return L_3;
			
	}	

	public static HashMap<String, HashMap<String, Float>> multithreaded_Lk(HashSet<List<String>> Ck, int k){
		/*
		 * Generate colocation of size 3
		 */
		long time1 = System.currentTimeMillis();
		
		HashMap<String, HashMap<String, Float>> Lk = new HashMap<String, HashMap<String,Float>>();
		System.out.println("Generating Colocations of Size "+k);
		
		Iterator iterator = Ck.iterator();
		int count = 0;
		while (iterator.hasNext()) {
			iterator.next();
			count++;			
		}
		ExecutorService executorService = null;
		if(count<24){
			executorService = Executors.newFixedThreadPool(count);
		}
		else{
			executorService = Executors.newFixedThreadPool(24);
		}
		
		Iterator it = Ck.iterator();
		
//		ExecutorService executorService = Executors.newFixedThreadPool(100);
		
		while(it.hasNext()){
			ValidateTraversal validateTraversal = new ValidateTraversal((List<String>) it.next(), k);
			executorService.execute(validateTraversal);
		}
		executorService.shutdown();
		while(!executorService.isTerminated()){
			;
		}
		System.out.println("All the threads terminated successfully");
		
		it = colocations.entrySet().iterator();
						
		while(it.hasNext()){
			Map.Entry<List<String>, Float> pairs = (Map.Entry<List<String>, Float>) it.next();
			List<String> tempList = (List<String>) pairs.getKey();
			String type1 = "";
			int i, j;
			for(i = 0; i < k-2; i++){
				type1 += tempList.get(i)+":";
			}
			type1 = type1.substring(0, type1.length()-1);
			String type2 = tempList.get(k-2);
			String type3 = tempList.get(k-1);
			
			if(Lk.containsKey(type1+":"+type2)==false){
				HashMap<String, Float> tempHashMap = new HashMap<String, Float>();
				tempHashMap.put(type3, pairs.getValue());
				Lk.put(type1+":"+type2, tempHashMap);
			}else{
				Lk.get(type1+":"+type2).put(type3, pairs.getValue());
			}			
		}
		colocations.clear();
		long time2 = System.currentTimeMillis();
		System.out.println("Total time for verifying itemsets of size "+k+" = "+(time2-time1));
		return Lk;
	}
	
	public static HashMap<String, HashMap<String, Float>> Lk(HashSet<List<String>> Ck){
		/*
		 * Generate colocations of size 2
		 * Iterate over all edges of the graph
		 */
		
		HashMap<String, HashMap<String, Float>> Lk = new HashMap<String, HashMap<String,Float>>();
		
		long time1 = System.currentTimeMillis();
		
		if(verbose){
			System.out.println("Generating colocations of size 3.\n");
		}
		int counter = 0;
		//could lead to buffer-overflow
		int k=0;
//		GremlinPipeline pipe = new GremlinPipeline();
		
		Iterator it;
		it = Ck.iterator();
//		String type = [];
		
//		String candidate, type1, type2, type3;
		while(it.hasNext())
		{
			ArrayList<String> type = new ArrayList<String>();
			HashMap<String, HashSet<Long>> unique = new HashMap<String, HashSet<Long>>();
			List<String> tempList = ((List<String>)it.next());
			k = tempList.size();
			for(int i=0;i<k;i++)
			{
				String key = tempList.get(i);
				if(unique.containsKey(key)==false)
				{
					HashSet<Long> tempHashSet = new HashSet<Long>();
					unique.put(key, tempHashSet);
				}
				type.add(key);
			}
//			candidate = type1+":"+type2+":"+type3;
			
//			System.out.println("Candidate being screwed right now is : "+candidate);
			
			final List<Vertex> temp = new ArrayList<Vertex>();
			GremlinPipeline pipe = new GremlinPipeline();
			
			
			if (k==4)
			{
				pipe.start(graph.getVertices("type",type.get(0))).sideEffect(new PipeFunction<Vertex, Vertex>(){
					public Vertex compute(Vertex argument){
						if(temp.size()>0){
							temp.remove(temp.size()-1);
						}
						temp.add(argument);
						return argument;
					}
					}).in(type.get(0)+"-"+type.get(1)).in(type.get(1)+"-"+type.get(2)).in(type.get(2)+"-"+type.get(3)).out(type.get(0)+"-"+type.get(3)).filter(new PipeFunction<Vertex,Boolean>() {
					  public Boolean compute(Vertex argument) {
						  if(temp.contains(argument)){
							  return true;
						  }
						  else{
							  return false;
						  }				  
						  }
						}).path(new PipeFunction<Vertex, Long>(){
					public Long compute(Vertex argument) {
						return (Long) argument.getId();
					}
				}).enablePath();
			}
//			else if (k==5) {
//				
//			}
				
			Iterator pit = pipe.iterator();
			
			while(pit.hasNext())
			{
//				System.out.println(pit.next().get(0).getClass());
				List<Long> IdList = ((List<Long>)pit.next());
				Boolean flag = true;
				outerloop:
				for (int i = 0; i < k-2; i++) 
				{
					for (int j = i+2; j < k; j++) 
					{
						if(areConnected(IdList.get(i), IdList.get(j)) == false)
						{
							flag = false;
							break outerloop;
						}
					}
				}
				if(flag==false)
					continue;
				for (int i = 0; i < k; i++) 
				{
					unique.get(type.get(i)).add(IdList.get(i));
				}
			}
			
			float ParticipationIndex = (float)1.0;
			for (int x = 0; x < k; x++) 
			{
				float ParticipationRatio = unique.get(type.get(x)).size()/((float)total_count.get(type.get(x))); 
				if(ParticipationIndex > ParticipationRatio)
				{
					ParticipationIndex = ParticipationRatio;
				}
			}
			if(ParticipationIndex >= PI_threshold)
			{
				String type1 = "";
				int i, j;
				for(i = 0; i < k-2; i++){
					type1 += type.get(i)+":";
				}
				type1 = type1.substring(0, type1.length()-1);
				String type2 = type.get(k-2);
				String type3 = type.get(k-1);
				//System.out.println(type1+":"+type2+":"+type3+" = "+ParticipationIndex);
				
				if(Lk.containsKey(type1+":"+type2)==false)
				{
					HashMap<String, Float> tempHashMap = new HashMap<String, Float>();
					tempHashMap.put(type3, ParticipationIndex);
					Lk.put(type1+":"+type2, tempHashMap);
				}else
				{
					Lk.get(type1+":"+type2).put(type3, ParticipationIndex);
				}
			}
		}
		
		if (verbose)
		{
			System.out.println("Total no. of colocations of size 2  are = "+counter);
		}
		long time2 = System.currentTimeMillis();
		System.out.println("Time taken for size "+k+" : "+(time2-time1));
		return Lk;
			
		}	
	
	public static void print_Candidate(HashSet<List<String>> Ck, int k){
		int counter = 0;
		System.out.println("Candidate Colocations of Size "+k);
		Iterator it,it1;
		it = Ck.iterator();
		while(it.hasNext()){
			List<String> tempList = ((List<String>)it.next());
			String candidate = "";
			for(int i= 0;i<tempList.size();i++){
				candidate = candidate+tempList.get(i)+":";				
			}
			candidate = candidate.substring(0, candidate.length()-1);
//			System.out.println(candidate);
			counter++;
		}		
		System.out.println();
		System.out.println("Total no.of of candidates of size "+k+" are "+counter);
	}
	
	public static void main(String[] args) {
		
		Baseline baseline = new Baseline();
		
		HashMap<String, HashMap<String, Float>> Lk;
		HashSet<List<String>> Ck = new HashSet<List<String>>();
		Ck = L1();
		print_Candidate(Ck, 2);
		
		for(int k = 2;k<11;k++){
			System.out.println("Current K is "+k);
			if(k==2){
				Lk = multithreaded_L2(Ck, 2);
//				Lk = L2();				
			}else if (k==3) {
				Lk = multithreaded_Lk(Ck, k);
//				Lk = L3(Ck);
			}else {
				Lk = multithreaded_Lk(Ck, k);
//				Lk = Lk(Ck);
			}
			print_Frequent(Lk, k);
			Ck = join_and_prune(Lk, k);
			print_Candidate(Ck, k+1);
			
			if(Ck.isEmpty()){
				break;
			}
		}
		
		
//		HashSet<List<String>> C2 = new HashSet<List<String>>();
//		HashMap<String, HashMap<String, Float>> L_k = new HashMap<String, HashMap<String, Float>>();
//		HashSet<List<String>> Ck = new HashSet<List<String>>();
//		
//		C2 = L1();
////		print_Candidate(C2, 2);
//		L_k = L2(C2); 
////		print_Frequent(L_k, 2);
//		Ck = join_and_prune(L_k, 2);
////		print_Candidate(Ck, 3);
//		L_k = L3(Ck);
//		print_Frequent(L_k, 3);
//		Ck = join_and_prune(L_k, 3);
//		print_Candidate(Ck, 4);
//		L_k = Lk(Ck);
//		print_Frequent(L_k, 4);
		
		
		db.closeTitanGraph(graph);		
	}

}
