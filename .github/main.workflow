workflow "Old flow" {
  on = "create"
  resolves = ["Setup Java Action"]
}

action "Setup Java Action" {
  uses = "actions/setup-java@204b974cf476e9709b6fab0c59007578676321c5"
}
