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
    req.onreadystatechange = function() {
      if( req.readyState == 4 ) {
        var thisdate = document.location.href.match(
                        /(\d\d\d\d-\d\d-\d\d)\.html/)[1];
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

        if( thisdateidx > -1 ) {
          if( thisdateidx > 0 ) {
            html.push( '<a href="', datelist[ thisdateidx - 1 ], '.html">&lt; ',
                datelist[ thisdateidx - 1 ], '</a>' );
          }
          else {
            html.push( '<span class="i">At earliest date &gt;&gt;</span>' );
          }

          html.push( ' &nbsp; &nbsp; &nbsp; ' );

          if( thisdateidx < datelist.length - 1 ) {
            html.push( '<a href="', datelist[ thisdateidx + 1 ], '.html">',
                datelist[ thisdateidx + 1 ], ' &gt;</a>' );
          }
          else {
            html.push( '<span class="i">At lastest date &gt;&gt;</span>' );
          }

          html.push( ' &nbsp; ' );
        }
        if( thisdateidx < datelist.length - 2 ) {
          html.push( '<a href="', lastdate, '.html">',
              lastdate, ' &gt;&gt;</a>' );
        }
        var htmlstr = html.join('');
        nav.innerHTML = htmlstr;
        document.getElementById('nav-foot').innerHTML = htmlstr;
      }
    };
    req.open("GET", "./", true);
    req.send("");
  }
  else {
    nav.style.color = "#855";
    nav.innerHTML = "loading navigation failed";
  }
}
