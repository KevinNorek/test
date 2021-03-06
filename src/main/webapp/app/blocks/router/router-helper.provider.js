(function() {
  'use strict';

  angular
    .module('blocks.router')
    .provider('routerHelper', routerHelperProvider);

  /* @ngInject */
  function routerHelperProvider($locationProvider, $stateProvider, $urlRouterProvider) {
    /* jshint validthis:true */
    var config = {
      docTitle: 'Fourcast - Template',
      resolveAlways: {}
    };

    $locationProvider.html5Mode(true);

    this.configure = function(cfg) {
      angular.extend(config, cfg);
    };

    this.$get = RouterHelper;

    /* @ngInject */
    function RouterHelper($location, $state, $rootScope, logger) {
      var handlingStateChangeError = false;
      var hasOtherwise = false;
      var stateCounts = {
        errors: 0,
        changes: 0
      };

      var service = {
        configureStates: configureStates,
        getStates: getStates,
        stateCounts: stateCounts
      };

      init();

      return service;

      function init() {
        handleRoutingErrors();
        updateDocTitle();
      }

      function configureStates(states, otherwisePath) {
        states.forEach(function(state) {
          state.config.resolve =
            angular.extend(state.config.resolve || {}, config.resolveAlways);
          $stateProvider.state(state.state, state.config);
        });
        if (otherwisePath && !hasOtherwise) {
          hasOtherwise = true;
          $urlRouterProvider.otherwise(otherwisePath);
        }
      }

      function getStates() {
        return $state.get();
      }

      /*
       * Route cancellation:
       * On routing error, go to the dashboard
       * Provide an exit clause if it tries to do it twice
       */
      function handleRoutingErrors() {
        $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error) {
          if (handlingStateChangeError) {
            return;
          }
          stateCounts.errors++;
          handlingStateChangeError = true;
          var destination = toState &&
            (toState.title || toState.name || toState.loadedTemplateUrl) || 'unknown target';
          var msg = 'Error routing to ' + destination + '. ' +
            (error.data || '') + '.<br/>' + (error.statusText || '') +
            ': ' + (error.status || '');
          logger.warning(msg, [toState]);
          $location.path('/');
        });
      }

      function updateDocTitle() {
        $rootScope.$on('$stateChangeSuccess', function(event, toState) {
          stateCounts.changes++;
          handlingStateChangeError = false;
          var title = config.docTitle + ' ' + (toState.title || '');
          $rootScope.title = title;
        });
      }
    }
  }

})();