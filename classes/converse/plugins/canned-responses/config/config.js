var defaultAnswers = [
  {
    name: 'Issue: thanks! help fix?',
    description: "Thanks a lot for filing this issue! Would you like to write a patch for this? We'd be more than happy to walk you through the steps involved."
  },
  {
    name: 'Issue: thanks! looking!',
    description: "Thanks a lot for filing this issue! We'll triage and take a look at it as soon as possible!"
  },
  {
    name: 'Issue: looks inactive',
    description: "This issue is fairly old and there hasn't been much activity on it. Closing, but please re-open if it still occurs."
  },
  {
    name: 'Issue: closing, no repro steps',
    description: "This issue has no reproducible steps. Please re-open this issue if it still occurs, with a JSBin containing a set of reproducible steps. Check this element's CONTRIBUTING.md for an example."
  },
  {
    name: 'Issue: provide repro steps',
    description: "Please provide a JSBin containing a set of reproducible steps. Check this element's CONTRIBUTING.md for an example."
  },
  {
    name: 'Issue: cannot reproduce',
    description: "Not reproducible in the latest release. Please re-open this issue if it still occurs, with a JSBin containing a set of reproducible steps. Check this element's CONTRIBUTING.md for an example."
  },
  {
    name: 'PR: thanks! looking!',
    description: "Thanks for your contribution! We'll triage and take a look at it as soon as possible!"
  },
  {
    name: 'PR: needs test',
    description: "Please add a test case that tests the problem this PR is fixing."
  }
];

var getAnswersListFromStorage  = function()
{
  // Load the answers from local storage.
  var localStorageKey = "store.settings.cannedResponses";
  var saved = localStorage.getItem(localStorageKey);
  var answers;

  if (!saved || saved === '') {
    localStorage.setItem(localStorageKey, JSON.stringify(defaultAnswers));
    answers = defaultAnswers;
  } else {
   answers = JSON.parse(localStorage.getItem(localStorageKey));
  }
  return answers;
}

