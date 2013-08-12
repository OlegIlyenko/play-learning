@CommentsCtrl = ($scope) ->
  $scope.comments = []
  $scope.viewerCount = 0
  $scope.errorMessage = ""

  $scope.init = (id) ->
    $scope.ws = new WebSocket(routes.controllers.Books.commentSocket(id).webSocketURL())

    $scope.ws.onmessage = (msg) ->
      $scope.errorMessage = ""
      message = JSON.parse(msg.data)

      $scope.$apply ->
        switch message.type
          when 'error' then $scope.error(message.message)
          when 'added' then $scope.comments.unshift message.comment
          when 'viewers' then $scope.viewerCount = message.count
          when 'list' then $scope.comments = message.comments
          else $scope.error("Unknown message type: #{message.type}")

    setInterval (-> $scope.$apply()), 3000

  $scope.formatDate = (d) ->
    moment(d).fromNow()

  $scope.hideError = ->
    $scope.errorMessage = ""

  $scope.error = (msg) ->
    $scope.errorMessage = msg

  $scope.addComment = ->
    if not not $scope.newCommentText
      $scope.ws.send JSON.stringify
        date: new Date
        comment: $scope.newCommentText

      $scope.newCommentText = ""