function onload() {
  var nav = document.getElementById('nav-head');

  var req = false;
  if(XMLHttpRequest) {
    try {
      req = new XMLHttpRequest();
    } catch(e) {
      req = false;
    }
  } else if(ActiveXObject) {
    try {
      req = new ActiveXObject("Msxml2.XMLHTTP");
    } catch(e) {
      try {
        req = new ActiveXObject("Microsoft.XMLHTTP");
      } catch(e) {
        req = false;
      }
    }
  }
  if(req) {
    var urlm = document.location.href.match(/(\d\d\d\d-\d\d-\d\d)\.html/);
    var thisdate = urlm && urlm[1];
    req.onreadystatechange = function() {
      if( req.readyState == 4 ) {
        var thisdateidx = -1;
        var datehash = {};
        var datelist = [];
        req.responseText.replace( /(\d\d\d\d-\d\d-\d\d)\.html/g, function(s,d) {
          if( ! datehash[ d ] ) {
            if( d == thisdate ) {
              thisdateidx = datelist.length;
            }
            datelist.push( d );
            datehash[ d ] = true;
          }
        });
        var lastdate  = datelist[ datelist.length - 1 ];
        var html = [];

        if( thisdateidx < 0 ) {
          thisdateidx = datelist.length - 1;
        }

        function pushlink( idx, text ) {
          if( idx < 0 || idx > datelist.length - 1 || thisdateidx == idx )
            html.push('<a class="i">');
          else
            html.push('<a href="/date/', datelist[ idx ], '.html"',
                ' title="', datelist[ idx ], '">');
          html.push( text, '</a>' );
        }

        pushlink( 0, "« Earliest" );
        pushlink( thisdateidx - 1, "‹ Previous" );
        pushlink( thisdateidx + 1, "Next ›" );
        pushlink( datelist.length - 1, "Latest »" );

        html.push( '&nbsp;' );

        var htmlstr = html.join('');
        nav.innerHTML = htmlstr;
        document.getElementById('nav-foot').innerHTML = htmlstr;
      }
    };
    req.open("GET", "/date/", true);
    req.send("");
  }
  else {
    nav.style.color = "#855";
    nav.innerHTML = "loading navigation failed";
  }
}
