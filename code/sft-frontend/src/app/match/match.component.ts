import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-match',
  templateUrl: './match.component.html',
  styleUrls: ['./match.component.css']
})
export class MatchComponent implements OnInit {

  constructor() { }

  ngOnInit() {
  }

  match: Match = {
    id: 1,
    date: '2018-19-05-12-30',
    durationInSec: 300,
    goalsHomeTeam: 6,
    goalsGuestTeam: 3
  };

}
