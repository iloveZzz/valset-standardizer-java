(function () {
  // 本地离线占位：当外网被禁或 CDN 不可达时，@ant-design/icons 的 createFromIconfontCN
  // 仍会尝试加载此文件，以便已存在的图标名不会导致运行时异常。
  // 你可以用真实从 Iconfont 下载的 symbol 脚本覆盖本文件内容。
  try {
    var svgSprite =
      '<svg><symbol id="icon-placeholder" viewBox="0 0 1024 1024"><path d="M512 85.333c235.989 0 426.667 190.677 426.667 426.667S747.989 938.667 512 938.667 85.333 747.989 85.333 512 276.011 85.333 512 85.333z m0 85.334c-188.544 0-341.333 152.789-341.333 341.333 0 188.544 152.789 341.333 341.333 341.333 188.544 0 341.333-152.789 341.333-341.333 0-188.544-152.789-341.333-341.333-341.333z m0 149.333a64 64 0 110 128 64 64 0 010-128z m0 149.334c35.346 0 64 28.654 64 64v170.666a64 64 0 11-128 0V533.334c0-35.346 28.654-64 64-64z" fill="#999"/></symbol></svg>';
    var script = (function () {
      var scripts = document.getElementsByTagName('script');
      return scripts[scripts.length - 1];
    })();
    var shouldInjectCss = script.getAttribute('data-injectcss');
    var ready = function (fn) {
      if (document.addEventListener) {
        if (~['complete', 'loaded', 'interactive'].indexOf(document.readyState)) {
          setTimeout(fn, 0);
        } else {
          var loadFn = function () {
            document.removeEventListener('DOMContentLoaded', loadFn, false);
            fn();
          };
          document.addEventListener('DOMContentLoaded', loadFn, false);
        }
      } else if (document.attachEvent) {
        IEContentLoaded(window, fn);
      }
    };
    var before = function (el, target) {
      target.parentNode.insertBefore(el, target);
    };
    var prepend = function (el, target) {
      if (target.firstChild) {
        before(el, target.firstChild);
      } else {
        target.appendChild(el);
      }
    };
    var appendSvg = function () {
      var div = document.createElement('div');
      div.innerHTML = svgSprite;
      svgSprite = null;
      var svg = div.getElementsByTagName('svg')[0];
      if (svg) {
        svg.setAttribute('aria-hidden', 'true');
        svg.style.position = 'absolute';
        svg.style.width = 0;
        svg.style.height = 0;
        svg.style.overflow = 'hidden';
        prepend(svg, document.body);
      }
    };
    if (shouldInjectCss && !window.__iconfont__svg__cssinject__) {
      window.__iconfont__svg__cssinject__ = true;
      try {
        document.write(
          '<style>.svgfont{display:inline-block;width:1em;height:1em;fill:currentColor;vertical-align:-0.1em;font-size:16px;}</style>'
        );
      } catch (e) {
        console.log(e);
      }
    }
    ready(appendSvg);
  } catch (e) {
    // 忽略占位脚本异常，避免影响业务
  }
})();
