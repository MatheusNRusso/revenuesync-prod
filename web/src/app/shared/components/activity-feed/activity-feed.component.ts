import { Component, Input } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import {Payment} from '../../../core/models/payment.model';

@Component({
  selector: 'app-activity-feed',
  standalone: true,
  imports : [CommonModule, DatePipe],
  templateUrl: './activity-feed.component.html',
  styleUrls: ['./activity-feed.component.scss']
})
export class ActivityFeedComponent {
  @Input() payments: Payment[] = [];
}
