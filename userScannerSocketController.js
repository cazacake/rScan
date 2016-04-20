var socket;
var Socket={						//the socket object
	setup:function (){
				logConsole("setup Called");
				var host = "wss://fierce-bayou-33827.herokuapp.com/";
				socket = new WebSocket(host);
				if (socket) {
					socket.onopen = function() {
						$(".notConnectedSpan").addClass("isConnectedSpan").removeClass("notConnectedSpan").text("Is Connected");

					}
					socket.onmessage = function(msg) {
						var data = JSON.parse(msg.data);
						var value = data['value'];
						var key = data['key'];
						var Event = data['event'];

						logConsole(key+" "+Event+" "+value);

						if(Event=="pageEvent"){
							$("#OutputParagraph").text($("#OutputParagraph").text()+"<br />"+data["value"]);
						}
						else if(Event=="printTest"){
							console.log("->"+value);
						}
						else if(Event=="errorMessage"){
							$(".outputHeader").text(data["value"]);
						}
						else if(Event=="Local"){								//store incoming data in localStorage
							localStorage.setItem("Local", value);	//
						}
						else if(Event=='startup'){
							//imgUrl=value;
						}
						else if(Event=="changeOutputHeader"){
							$(".outputHeader").text("CheckingValidity");
						}
						else if(Event=="clearOutputField"){
							$("#OutputParagraph").text("");
							$(".outputHeader").text("Valid User");

						}
						else if(Event=="message"){
							$("#returnTextField").text(
								data['value']);
						}
						else if(Event=="returnReport"){
							console.log(data["subredditHits"]);

							$(".outputHeader").text("Output");
							$("#OutputParagraph").text($("#OutputParagraph").text()+
							"<br>Username:"+data["username"]+
							"<br>Default Scan:"+data["doDefault"]+
							"<br>Filter Group Hits:"+data["flaggedFilterGroupSubsCount"]+
							"<br>Flagged Total:"+data["flaggedCount"]);
							if(data["doDefault"]===true){
								$("#OutputParagraph").text($("#OutputParagraph").text()+
								"<br>DefaultScan Submission Count:"+data["flaggedSubmissionCount"]+
								"<br>DefaultScan Comments Count:"+data["flaggedCommentsCount"]+
								"<br>Flag Level:"+data["flagLevel"]
								);
							}
						}

					}
					socket.onclose = function() {
						logConsole("Socket Closed");//console.log("SocketClosed");
						$(".outputHeader").text("Output");
						$(".isConnectedSpan").addClass("notConnectedSpan").removeClass("isConnectedSpan").text("Not Connected");

						setTimeout(function(){
								Socket.setup();
						}, 300);
					}
				} else {
					logConsole("invalid socket");
				}
			},

			getValue:function(key){		//get value from server
				var msg={event:"valueRequest",key:key};
				sendMessage(msg);
			},

			sendMessage:function(msg) {//accepts a json strings
				waitForSocketConnection(socket, function() {
						socket.send(msg);
				});
		},
		sendJsonValue:function(val,doEscape){//val is a json object,
			if((typeof val.key)=="string" && doEscape==true){
				//val.key=RegExp.unescape(val.key);
			}
			this.sendMessage(JSON.stringify(val));
		},
		sendServerMessage:function(key,value,Event){
			//if addExtended path ==true, add escaped(smartDashboard) to key
				if((typeof key)=="string"){
						//key=RegExp.unescape(key);
				}
			var val={
				"key":key,
				"value":value,
				"action":Event
			}
			this.sendMessage(JSON.stringify(val));
		}
}

var showLogs=true;
function logConsole(message){
	if(showLogs==true){
	console.log(message);}
}
RegExp.escape = function(text) {
	if (!arguments.callee.sRE) {
		var specials = [
			'/', '.', '*', '+', '?', '|',
			'(', ')', '[', ']', '{', '}', '\\'
		];
		arguments.callee.sRE = new RegExp(
			'(\\' + specials.join('|\\') + ')', 'g'
		);
	}
	return text.replace(arguments.callee.sRE, '\\$1');
}
RegExp.unescape=function(text){
	return text.replace(/\\/g,"")
}
waitForSocketConnection=function(sock,callback){
	//Callback to make sure it waits for finished connection before it sends messages
	setTimeout(
				function(){
						if (sock.readyState === 1) {
								if(callback !== undefined){
										callback();
								}
								return;
						} else {
					waitForSocketConnection(sock,callback);
						}
				}, 5);
}


jQuery(function($) {
	if (!("WebSocket" in window)) {
		alert("Your browser does not support web sockets");
	} else {
		Socket.setup();
	}

});
