var socket = "{{socket}}";
var token = "{{token}}"
var connection = new WebSocket(socket);

$(document).ready(function () {
    $('.tabs').tabs();
    $('#modal').modal({
        onCloseEnd: function () {
            $("#songsRes ul").empty();
        }
    });
    addOn(5);
});

$("#searchForm").submit(function (e) {
    e.preventDefault();
});
function updateImage() {

}
function addOn(maxTries) {
    connection = new WebSocket(socket);
    connection.onmessage = function (m) {
        msg = JSON.parse(m.data);
        console.log(msg);
        if (Object.keys(msg).length !== 0) {
            (msg["is_paused"]) ? document.getElementById("PlayIcon").innerHTML = "play_arrow" : document.getElementById("PlayIcon").innerHTML = "pause";
            document.getElementById("SongName").innerHTML = msg["track"]["name"];
            document.getElementById("SongDescription").innerHTML = "By " + msg["track"]["artist"]["name"]
            document.getElementById("albumcover").src = "/api/current-image"
            populateQueue(JSON.parse(msg.queue));
        } else {
            document.getElementById("albumcover").src = "/api/current-image"
        }
    };
    connection.onclose = function m(uri, protocol) {
        console.log("on close");
        let ele = document.getElementById("conStatusBar");
        ele.classList.remove("green");
        ele.classList.add("red");

        console.log("Max tries " + maxTries);
        if (maxTries >= 0) {
            setTimeout(function () {
                document.getElementById("conStatusBarMsg").innerText = "Trying to reconnect..." + maxTries + " tries left";
                console.log("Trying to connect again. " + maxTries + " tries left");
                addOn(--maxTries);
            }, 1000);
        }


    };
    connection.onopen = function (event) {
        maxTries = 10;
        let ele = document.getElementById("conStatusBar");
        ele.classList.remove("red");
        ele.classList.add("green");
        document.getElementById("conStatusBarMsg").innerText = "Connected to server";

    };
}

function onPrevious() {
    connection.send(JSON.stringify({'payload': 'previous'}));
}

function search() {
    let q = $("#songSearch").val();
    document.getElementById("searchArea").style.visibility = "hidden";
    $.ajax({
        url: "https://api.spotify.com/v1/search?q=" + q + "&type=track&limit=15",
        headers: {
            'Authorization': 'Bearer ' + token
        },
        success: function (data) {
            var songs = data.tracks.items;
            for (i of songs) {
                console.log(i);
                let res =
                    '<li>' +
                    '<div class="row">' +
                    '    <div class="card horizontal grey">' +
                    '      <div class="card-image">' +
                    '        <img src="' + i.album.images[0].url + '" alt="result" height="128">' +
                    '      </div>' +
                    '      <div class="card-stacked">' +
                    '        <div class="card-content">' +
                    '          <p>' + i.name + ' by ' + i.artists[0].name + '</p>' +
                    '        </div>' +
                    '        <div class="card-action black-text">' +
                    '           <a href="#" onclick="playURI(\'' + i.uri + '\')" >Add to Queue<i  class="material-icons right">add</i></a>' +
                    '        </div>' +
                    '      </div>' +
                    '    </div>' +
                    '  </div>' +
                    '</div>' +
                    '</li>';


                $("#songsRes ul").append(res);
            }
            songs = [];
            $('#modal').modal('open');
            document.getElementById("searchArea").style.visibility = "visible";
        },
        error: function () {
            document.getElementById("searchArea").style.visibility = "visible";
        }
    });


}

function onNext() {
    connection.send(JSON.stringify({'payload': 'next'}));
}

function onPlay() {
    connection.send(JSON.stringify({'payload': 'play'}));
}

function onQueue() {
    try {
        regex = /(track\/)([0-9A-z]*)/;
        uri = "spotify:track:" + regex.exec(document.getElementById("song_uri").value)[2];
        connection.send(JSON.stringify({'payload': 'playUri', 'uri': uri}));
        document.getElementById("song_uri").value = "";
    } catch (err) {
        alert("Invalid link or socket error");
    }
}

function playURI(uri) {
    connection.send(JSON.stringify({'payload': 'playUri', 'uri': uri}));
    $('#modal').modal('close');
}

function populateQueue(queue) {
    let trackIds = "";

    for (let uriQ of queue) {
        trackIds = trackIds + ((trackIds.length === 0) ? "" : ",") + uriQ.split(":")[2];
    }
    if (trackIds.length !== 0) {
        $.ajax({
            url: "https://api.spotify.com/v1/tracks/?ids=" + trackIds,
            headers: {
                'Authorization': 'Bearer ' + token
            },
            success: function (data) {
                let res = "";
                for (let tracks of data.tracks) {

                    res = res + " <li class=\"collection-item avatar grey darken-1\">\n" +
                        "                    <img height=\"64\" src=" + tracks.album.images[0].url + " alt=\"\" class=\"circle\">\n" +
                        "                    <span class=\"title\">" + tracks.name + "</span>\n" +
                        "                    <p>By " + tracks.artists[0].name + " </p>\n" +
                        "                </li>";
                }
                document.getElementById("queueCollection").innerHTML = res;
            }
        });
    } else {
        let res = '<li class="collection-item avatar grey darken-1">\n' +
            '                        <img height="64" src="http://pixelartmaker.com/art/8e901395c4a3dd4.png" alt="" class="circle">\n' +
            '                        <span class="title">Nothing Added Yet</span>\n' +
            '                        <p>Add a song and it will appear here!</p>\n' +
            '                    </li>';
        document.getElementById("queueCollection").innerHTML = res;
    }
}