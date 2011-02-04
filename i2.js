// clojure-irc-log/irc.js
// Copyright 2010 Chris Houser
// Please ask before using in other projects

function resize() {
  // JavaScript is running, so convert page from vertical scrolling to
  // horizontal paging:
  $('#frame').addClass('paged');
  $('#columns').addClass('paged');
  $(document.body).height($(window).height() - 20);

  // Insert initial per-column header and footer to establish their vertical
  // size so the page_count can be calculated correctly
  $('#columns').before('<header><table class="page_head"><tr><td class="page_head_left"></td><td class="page_head_right"></td></tr></table></header>');
  $('.page_head_left' ).append($('h1').contents().clone());
  $('.page_head_right').append($('h2').contents().clone());
  $('h1').css('margin-top', 0);
  var page_head = $('.page_head').clone();
  $('.page_head').css('visibility', 'hidden');

  $('#columns').after('<footer><table class="page_foot"><tr><td class="page_foot_left"><button onclick="scrollBy(-1)">&lt;&lt;&lt;</button></td><td class="page_foot_mid"></td><td class="page_foot_right"><button onclick="scrollBy(1)">&gt;&gt;&gt;</button></td></tr></table></footer>');
  var page_foot = $('.page_foot');
  $('header').height($('header').outerHeight());
  $('#columns').height(
      $('#frame').innerHeight()
      - $('header').outerHeight()
      - $('footer').outerHeight());

  // Measure widths
  var doc_width = window.doc_width = $('#frame')[0].scrollWidth;
  var col_width = window.col_width = $('#frame').innerWidth() - 1; //not quite right
  var page_count = Math.ceil(doc_width / col_width);

  // Make #wide exactly as wide as all the columns it contains
  // This not only sets up the container for headers and footers, but works
  // around a rendering bug in chrome
  $('#wide').css('width', doc_width + 'px');

  // Insert per-column headers and footers
  for(var i = 2; i < page_count; ++i) {
    $('header').append(page_head.clone());
    $('footer').append(page_foot.clone());
  }

  // Fix page numbers
  $('.page_foot_mid').each(function(i, elem) {
    elem.innerHTML = 'page ' + (i+1) + ' of ' + (page_count-1);
  });
}

window.scroll_anim = {
  goal_page: 0,
  velocity: 0,
  prev_draw: null
};

function doanim() {
  var anim = window.scroll_anim;
  var frame = $('#frame')[0];
  var goal_px = anim.goal_page * window.col_width;
  var diff = goal_px - frame.scrollLeft;
  var pull_force = (diff == 0 ? 20 :
                     (diff * diff * 0.02 + 20) * (diff / Math.abs(diff)));
  var velocity = anim.velocity * 0.5 + pull_force;  // friction
  anim.our_scroll = true;
  if(Math.abs(velocity) < 20 && Math.abs(diff) < 2) {
    frame.scrollLeft = goal_px;
    velocity = 0;
    anim.prev_draw = null;
  }
  else {
    var now = new Date();
    var elapsed = anim.prevdraw ? (now - anim.prev_draw) : 30;
    anim.prev_draw = now;
    frame.scrollLeft += velocity * elapsed * 0.001;  // velocity factor
    setTimeout(doanim, 20);
  }
  anim.velocity = velocity;
}

function scrollBy(p) {
  var anim = window.scroll_anim;
  var old_goal_page = anim.goal_page;
  var page_count = Math.ceil(window.doc_width / window.col_width);
  anim.goal_page = Math.max(0, Math.min(Math.round(old_goal_page + p * 0.6),
                                        page_count-2));
  if(anim.velocity == 0 && anim.goal_page != old_goal_page) {
    // not scrolling, so kick off a animation loop
    doanim();
  }
}

$(document).keydown(function(e) {
  var p = ({
    39:  1, 32:  1, 34:  1, 40:  1,
    37: -1, 33: -1, 38: -1
  })[e.which];
  if(p) scrollBy(p);
});

$('#frame').scroll(function(e) {
  var anim = window.scroll_anim;
  if(!anim.our_scroll) {
    anim.goal_page = $('#frame')[0].scrollLeft / window.col_width;
  }
  else {
    anim.our_scroll = false;
  }
});

$(document).ready(resize);
//$(document).resize(resize);
