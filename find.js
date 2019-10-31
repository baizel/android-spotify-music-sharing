//https://ourcodeworld.com/articles/read/257/how-to-get-the-client-ip-address-with-javascript-only
console.log("Pls")

/**
 * Get the user IP throught the webkitRTCPeerConnection
 * @param onNewIP {Function} listener function to expose the IP locally
 * @return undefined
 */
function getUserIP(onNewIP) { //  onNewIp - your listener function for new IPs
    //compatibility for firefox and chrome
    var myPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection;
    var pc = new myPeerConnection({
            iceServers: []
        }),
        noop = function () {
        },
        localIPs = {},
        ipRegex = /([0-9]{1,3}(\.[0-9]{1,3}){3}|[a-f0-9]{1,4}(:[a-f0-9]{1,4}){7})/g,
        key;

    function iterateIP(ip) {
        if (!localIPs[ip]) onNewIP(ip);
        localIPs[ip] = true;
    }

    //create a bogus data channel
    pc.createDataChannel("");

    // create offer and set local description
    pc.createOffer().then(function (sdp) {
        sdp.sdp.split('\n').forEach(function (line) {
            if (line.indexOf('candidate') < 0) return;
            line.match(ipRegex).forEach(iterateIP);
        });

        pc.setLocalDescription(sdp, noop, noop);
    }).catch(function (reason) {
        // An error occurred, so handle the failure to connect
    });

    //listen for candidate events
    pc.onicecandidate = function (ice) {
        if (!ice || !ice.candidate || !ice.candidate.candidate || !ice.candidate.candidate.match(ipRegex)) return;
        ice.candidate.candidate.match(ipRegex).forEach(iterateIP);
    };
}

var isFound = false;

function findServers(port, ipBase, ipLow, ipHigh, maxInFlight, timeout, cb) {
    var ipCurrent = +ipLow, numInFlight = 0, servers = [];
    ipHigh = +ipHigh;

    function tryOne(ip) {
        ++numInFlight;
        var address = "ws://" + ipBase + ip + ":" + port;
        var socket = new WebSocket(address);
        var timer = setTimeout(function () {
            console.log(address + " timeout");
            var s = socket;
            socket = null;
            s.close();
            --numInFlight;
            next();
        }, timeout);
        socket.onopen = function () {
            if (socket) {
                isFound = true;
                console.log(address + " success");
                clearTimeout(timer);
                servers.push(socket.url);
                --numInFlight;
                socket.close();
                next();

            }
        };
        socket.onerror = function (err) {
            if (socket) {
                console.log(address + " error");
                clearTimeout(timer);
                --numInFlight;
                next();
            }
        }
    }

    function next() {
        while (ipCurrent <= ipHigh && numInFlight < maxInFlight && !isFound) {
            tryOne(ipCurrent++);
        }
        if (isFound) {
            cb(servers);
        }
        // if we get here and there are no requests in flight, then
        // we must be done
        if (numInFlight === 0) {
            cb(servers);
        }
    }

    next();
}

var ipRegx = /\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b/;

function find(timeout) {
    getUserIP(function (ip) {
        let ips = ip.toString().split(".");
        let ipBase = ips[0] + "." + ips[1] + "." + ips[2] + ".";
        findServers(6969, ipBase, 1, 255, 20, timeout, function (servers) {
            console.log(" " + servers);
            if (servers.length !== 0) {
                let serverIp = '<a href="http://' + servers[0].match(ipRegx) + ':8080">Server Found!</a>';
                document.getElementById("server").innerHTML = serverIp;
            } else {
                document.getElementById("server").innerHTML = "Could not find a server :(";
            }
        });
    });
}

find(6000)
// var timeout = 1000;
//
// function loop() {
//     let fun = function (server) {
//         if (servers.size === 0) {
//             timeout = timeout * 2;
//             loop();
//         }
//     };
//     find(timeout, fun);
// }
