# SADA: Towards Self-Adaptive Dynamic Analysis
-----------
Balancing cost (e.g., scalability) and effectiveness (e.g., precision) is a fundamental challenge in program
analysis in general, often demanding the expensive effort of manually tuning the cost-effectiveness trade-off
for each program under analysis. For dynamic analysis, in particular, the manual tuning effort can be even
greater as it may be needed for each particular execution of the program. This manual approach may not even
be possible when the program execution lasts very long (or even uninterruptedly) while the characteristics of
the execution vary over time—the execution dynamics also affect the trade-off.
In this paper, we explore self-adaptive dynamic analysis, a new dynamic analysis methodology that automatically
tunes the analysis’s algorithmic configuration hence balances its cost-effectiveness trade-off according
to the various program under analysis, different execution of each program, and varying characteristics of
each program execution over time. The core idea is that the analysis itself includes a dedicated controller that
is initialized for a given particular program execution and then continually adapts the configuration to the
execution dynamics on the fly, towards achieving and maintaining optimal cost-effectiveness balance with
respect to a given cost budget.
We instantiate our methodology in the context of dynamic dependence analysis for distributed systems
as a demonstrating example, as these systems commonly feature long/continuous executions to provide
uninterrupted services. Under a particular configuration space, we study and compare three strategies,
reinforcement learning (RL), deep RL (DRL), and adaptive control (CTL), for self-adapting the dynamic analyzer.
We evaluated our methodology through this example dynamic analysis against 12 real-world distributed
system executions and showed its significant superiority over the current state-of-the-art peer analysis. We
also extensively compared the three self-adaptation strategies and discussed the implications of the results to
the future design of self-adaptive dynamic analysis. While only demonstrated in its application for dependence
computation for distributed programs, our methodology (as the key contribution) is orthogonal to the analysis
(dependence computation) algorithm itself. Thus, we expect this methodology to be widely applicable to other
dynamic analyses, especially those for long/continuously running programs.

The complete artifact package has been made available at https://github.com/baltsers/SADA,
including the code, experimental scripts, and datasets. 
The operations and test inputs of our subjects are documented in the file "Inputs.txt".	
										
-----------
### Install DistODD, an instance of this methodology 
-----------
      
- Download files from https://github.com/baltsers/SADA

- Copy all library files from the directory ”tool” of DistODD to a directory (e.g., "lib") defined by the user.


-----------
### Download and install subjects
-----------

- MultiChat https://code.google.com/p/multithread-chat-server/

- NIOEcho   http://rox-xmlrpc.sourceforge.net/niotut/index.html#The code

- OpenChord https://sourceforge.net/projects/open-chord/files/Open%20Chord%201.0/

- Thrift	http://archive.apache.org/dist/thrift/

- xSocket	https://mvnrepository.com/artifact/org.xsocket/xSocket

- Voldemort https://github.com/apache/zookeeper/releases

- ZooKeeper https://github.com/apache/zookeeper/releases

- Netty	  https://bintray.com/netty/downloads/netty/


-----------
### Compute dependencies
-----------

#### 1. Select one subject.

- NioEcho is a simple system whose server echoes all messages from the clients. 

- MultiChat is a chat program whose clients broadcast their messages to all other clients through the server. 

- OpenChord provides peer-to-peer network services using distributed hash tables. 

- Thrift is an application development framework with a code generation engine for developing scalable cross-language services. 

- xSocket is a framework based on non-blocking IO (NIO), for constructing high-performance, scalable software systems. 

- Voldemort is a distributed key-value storage system used by LinkedIn.

- ZooKeeper is a coordination system providing distributed synchronization and group services. 

- Netty is an asynchronous NIO framework used to rapidly develop server/client network applications. 
			 
#### 2. Use DistODD to compute dependencies.
      
- 2.1  Step 1 (Phase 1): Instrumentation:

  We execute code/shell/#subject#/ODDInstr.sh (e.g., code/shell/xSocket/ODDInstr.sh).  

- 2.2  Step 2 (Phase 2): Pre-training:				
			
  We use the existing testing data of the subject.
    For example, we copy the testing data files from the directory ”data/#subject#” (data/xSocket) of the package to the subject directory (e.g., xSocket) defined by the user.
	
  In particular, for library subjects(Thrift, xSocket, and Netty), we also compile corresponding applications developed by us.
    For example, we compile all java files in the directory ”data/#subject#/java” (data/xSocket/java).
			
- 2.3  Step 3 (Phases 3 and 4): Arbitration and Adjustment:		  
						
  First, we set milliseconds (e.g., 40000 for xSocket) for a user budget constraint in the file "budget.txt".
			
  Second, we start server and client instances of the instrumented program.
	For example, for a xSocket integration test, we separately execute "./serverODD.sh", "./clientODD.sh", and "./client2ODD.sh" to start a server and two clients of the instrumented program. 
	These two clients automatically send text messages to the server. 
	For continuously executing the xSocket integration test, we execute "./clientODDTimes.sh" and "./client2ODDTimes.sh" with parameters "(client execution) times" (e.g., 999).
			
  Finally, analysis configurations vary according to our control-based strategy.
		 
- 2.4  Step 4 (Phase 5): User Interaction:  

  First, we execute code/shell/#subject#/ODDQueryClient.sh (e.g., code/shell/xSocket/ODDQueryClient.sh) to start a querying client.

  Then, we input a dependence query (i.e., method name) such as <org.xsocket.connection.IoSocketDispatcher: void run()>.

  Eventually, we get corresponding dependencies as the outputs of DistODD.
	
