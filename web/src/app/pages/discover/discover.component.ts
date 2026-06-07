import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService }          from '../../core/services/auth.service';
import { DiscoverService }       from '../../core/services/discover.service';
import { PublicProfileService }  from '../../core/services/public-profile.service';
import { PublicMerchant }        from '../../core/models/discover/public-merchant.model';
import { PublicProfile, ProfileCategory, CATEGORY_LABELS } from '../../core/models/discover/public-profile.model';

type MerchantSource = 'LIVE' | 'FEATURED';

interface FeaturedMerchant {
  id: string; name: string; slug: string;
  description?: string; avatarUrl?: string; category?: string;
}

interface MarketplaceMerchant {
  id: string; name: string; slug: string;
  description?: string; avatarUrl?: string;
  category: string; source: MerchantSource;
}

@Component({
  selector: 'app-discover',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './discover.component.html',
  styleUrls: ['./discover.component.scss'],
})
export class DiscoverComponent implements OnInit, OnDestroy {

  // ── Merchant state ────────────────────────────────────────────────────────
  loading     = false;
  liveLoading = false;
  liveError: string | null = null;

  query          = '';
  activeCategory = 'All';
  tps            = 2847;

  featuredMerchants:    FeaturedMerchant[]   = [];
  liveMerchants:        PublicMerchant[]      = [];
  marketplaceMerchants: MarketplaceMerchant[] = [];
  filteredMerchants:    MarketplaceMerchant[] = [];

  categories: string[] = [
    'All', 'Live Network', 'Showcase',
    'Store', 'Gaming', 'Art', 'Food', 'Tech', 'Music',
  ];

  // ── Profile state ─────────────────────────────────────────────────────────
  profilesLoading = false;
  profilesError: string | null = null;
  profiles: PublicProfile[] = [];
  profileSearch  = '';
  profileCategory: ProfileCategory | 'All' = 'All';


  readonly profileCategories: Array<{ value: ProfileCategory | 'All'; label: string }> = [
    { value: 'All',               label: 'All Builders'  },
    { value: 'BACKEND_DEVELOPER', label: 'Backend'       },
    { value: 'FRONTEND_DEVELOPER',label: 'Frontend'      },
    { value: 'FULLSTACK_DEVELOPER',label: 'Full Stack'   },
    { value: 'DEVOPS',            label: 'DevOps'        },
    { value: 'BLOCKCHAIN_WEB3',   label: 'Web3'          },
    { value: 'DATA_ML',           label: 'Data / ML'     },
    { value: 'DESIGNER',          label: 'Designer'      },
    { value: 'BUSINESS_FOUNDER',  label: 'Founder'       },
    { value: 'AGENCY_STUDIO',     label: 'Agency'        },
  ];

  readonly CATEGORY_LABELS = CATEGORY_LABELS;

  // ── Shared ────────────────────────────────────────────────────────────────
  marqueeItems: string[] = [
    'Solana Pay attribution live', '~400ms confirmation',
    '$0.00025 average fee', 'Non-custodial payments',
    'Meta CAPI integrated', 'Google Ads ready',
    'Mainnet-beta', 'Open source', 'Built for builders',
  ];

  private categoryColors: Record<string, string> = {
    Store: '#00d4aa', Gaming: '#9945FF', Art: '#f472b6',
    Food: '#fbbf24', Tech: '#60a5fa', Music: '#a78bfa',
    'Live Network': '#00d4aa', Showcase: '#9945FF', All: '#00d4aa',
  };

  private categoryEmojis: Record<string, string> = {
    Store: '🛍️', Gaming: '🎮', Art: '🎨',
    Food: '🍜', Tech: '💻', Music: '🎧',
  };

  private readonly avatarGradients: Array<[string, string]> = [
    ['#00d4aa', '#0094ff'], ['#9945FF', '#00d4aa'], ['#f472b6', '#9945FF'],
    ['#fbbf24', '#f472b6'], ['#60a5fa', '#00d4aa'], ['#a78bfa', '#60a5fa'],
    ['#00f0c2', '#9945FF'],
  ];

  private merchantCategoryCache = new Map<string, string>();
  private tpsTimer?: number;

  constructor(
    public  readonly router:               Router,
    public  readonly authService:          AuthService,
    private readonly discoverService:      DiscoverService,
    private readonly publicProfileService: PublicProfileService,
    private readonly cdr:                  ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadFeaturedMerchants();
    this.loadLiveMerchants();
    this.loadProfiles();
    this.tpsTimer = window.setInterval(() => {
      this.tps = 2400 + Math.floor(Math.random() * 1400);
    }, 2000) as unknown as number;
  }

  ngOnDestroy(): void {
    if (this.tpsTimer) clearInterval(this.tpsTimer);
  }

  // ── Profile loading ───────────────────────────────────────────────────────

  loadProfiles(): void {
    this.profilesLoading = true;
    this.profilesError   = null;
    const cat = this.profileCategory === 'All' ? undefined : this.profileCategory;
    const q   = this.profileSearch.trim() || undefined;
    this.publicProfileService.listProfiles(cat, q).subscribe({
      next: (data) => { this.profiles = data; this.profilesLoading = false; },
      error: ()    => { this.profilesError = 'Failed to load profiles.'; this.profilesLoading = false; },
    });
  }

  setProfileCategory(cat: ProfileCategory | 'All'): void {
    this.profileCategory = cat;
    this.loadProfiles();
  }

  onProfileSearch(): void { this.loadProfiles(); }


  // ── Profile helpers ───────────────────────────────────────────────────────

  getProfileGradient(profile: PublicProfile): string {
    const pair = this.avatarGradients[this.hash(profile.slug) % this.avatarGradients.length];
    return `linear-gradient(135deg, ${pair[0]} 0%, ${pair[1]} 100%)`;
  }

  getProfileInitials(profile: PublicProfile): string {
    const name  = profile.displayName || profile.githubUsername || '?';
    const words = name.trim().split(/\s+/).filter(Boolean);
    if (words.length >= 2) return (words[0][0] + words[1][0]).toUpperCase();
    return name.slice(0, 2).toUpperCase();
  }

  getCategoryLabel(cat: ProfileCategory): string {
    return CATEGORY_LABELS[cat] ?? cat;
  }

  // ── Merchant methods (unchanged) ──────────────────────────────────────────

  onPayNow(slug: string): void {
    const checkoutPath = `/solana/checkout?slug=${encodeURIComponent(slug)}&context=buyer`;
    if (this.authService.isAuthenticated()) {
      this.router.navigateByUrl(checkoutPath);
    } else {
      this.router.navigate(['/login'], { queryParams: { redirect: checkoutPath } });
    }
  }

  onMerchantDetail(merchantId: number): void {

  onMerchantDetailFromString(merchantId: string): void {
    this.onMerchantDetail(Number(merchantId));
  }
    this.router.navigate(['/merchant', merchantId]);
  }

  getAvatarInitials(merchant: MarketplaceMerchant): string {
    const words = merchant.name.trim().split(/\s+/).filter(Boolean);
    if (words.length >= 2) return (words[0][0] + words[1][0]).toUpperCase();
    return merchant.name.slice(0, 2).toUpperCase();
  }

  getAvatarGradient(merchant: MarketplaceMerchant): string {
    const pair = this.avatarGradients[this.hash(merchant.id) % this.avatarGradients.length];
    return `linear-gradient(135deg, ${pair[0]} 0%, ${pair[1]} 100%)`;
  }

  loadFeaturedMerchants(): void {
    this.loading = true;
    this.featuredMerchants = [
      { id: 'featured-1', name: 'Helio Store',    slug: 'helio',   description: 'Digital goods store accepting Solana Pay.',       category: 'Store'  },
      { id: 'featured-2', name: 'Star Atlas',     slug: 'staratl', description: 'Open-world space exploration MMO on Solana.',     category: 'Gaming' },
      { id: 'featured-3', name: 'Magic Eden Lab', slug: 'melab',   description: 'NFT drops and collectibles marketplace.',         category: 'Art'    },
      { id: 'featured-4', name: 'Solana Sushi',   slug: 'sushi',   description: 'Pay your tab with SOL — instant settlement.',     category: 'Food'   },
      { id: 'featured-5', name: 'Phantom Tools',  slug: 'phantom', description: 'Developer tooling and SDKs for Web3 builders.',   category: 'Tech'   },
      { id: 'featured-6', name: 'Audius Live',    slug: 'audius',  description: 'Independent artists, paid directly per stream.',  category: 'Music'  },
      { id: 'featured-7', name: 'Tensor Trade',   slug: 'tensor',  description: 'Pro-grade NFT trading terminal.',                 category: 'Art'    },
      { id: 'featured-8', name: 'Jito Coffee',    slug: 'jito',    description: 'Specialty roasters — pay with USDC.',             category: 'Food'   },
      { id: 'featured-9', name: 'Backpack Build', slug: 'backpck', description: 'Builder collective shipping xNFT apps.',          category: 'Tech'   },
    ];
    this.loading = false;
    this.rebuildMarketplace();
  }

  loadLiveMerchants(): void {
    this.liveLoading = true;
    this.liveError   = null;
    this.discoverService.getPublicMerchants().subscribe({
      next: (data) => {
        this.liveMerchants = data || [];
        this.liveLoading   = false;
        this.rebuildMarketplace();
      },
      error: () => {
        this.liveError     = 'Failed to load live merchants.';
        this.liveMerchants = [];
        this.liveLoading   = false;
        this.rebuildMarketplace();
      },
    });
  }

  rebuildMarketplace(): void {
    const live: MarketplaceMerchant[] = this.liveMerchants.map((m) => ({
      id: `live-${m.id}`, name: m.name, slug: m.slug,
      description: m.description || 'Accepting Solana Pay payments through RevenueSync.',
      avatarUrl: m.avatarUrl, category: 'Live Network', source: 'LIVE' as MerchantSource,
    }));
    const featured: MarketplaceMerchant[] = this.featuredMerchants.map((m) => ({
      id: m.id, name: m.name, slug: m.slug, description: m.description,
      avatarUrl: m.avatarUrl,
      category: m.category || this.getCategoryForMerchant(m.name),
      source: 'FEATURED' as MerchantSource,
    }));
    this.marketplaceMerchants = [...live, ...featured];
    this.applyFilter();
  }

  setCategory(category: string): void { this.activeCategory = category; this.applyFilter(); }

  applyFilter(): void {
    const q = this.query.trim().toLowerCase();
    this.filteredMerchants = this.marketplaceMerchants.filter((m) => {
      const matchCat =
        this.activeCategory === 'All' ||
        (this.activeCategory === 'Live Network' && m.source === 'LIVE') ||
        (this.activeCategory === 'Showcase'     && m.source === 'FEATURED') ||
        m.category === this.activeCategory;
      const matchQ = !q ||
        m.name.toLowerCase().includes(q) || m.slug.toLowerCase().includes(q) ||
        m.category.toLowerCase().includes(q) || m.source.toLowerCase().includes(q) ||
        (m.description || '').toLowerCase().includes(q);
      return matchCat && matchQ;
    });
  }

  getCategoryCount(cat: string): number {
    if (cat === 'All')          return this.marketplaceMerchants.length;
    if (cat === 'Live Network') return this.marketplaceMerchants.filter((m) => m.source === 'LIVE').length;
    if (cat === 'Showcase')     return this.marketplaceMerchants.filter((m) => m.source === 'FEATURED').length;
    return this.marketplaceMerchants.filter((m) => m.category === cat).length;
  }

  getCategoryForMerchant(name: string): string {
    if (this.merchantCategoryCache.has(name)) return this.merchantCategoryCache.get(name)!;
    const explicit = this.featuredMerchants.find((m) => m.name === name)?.category;
    if (explicit && this.categoryColors[explicit]) {
      this.merchantCategoryCache.set(name, explicit); return explicit;
    }
    const cats   = ['Store', 'Gaming', 'Art', 'Food', 'Tech', 'Music'];
    const picked = cats[this.hash(name) % cats.length];
    this.merchantCategoryCache.set(name, picked);
    return picked;
  }

  getCategoryColor(m: MarketplaceMerchant): string {
    return m.source === 'LIVE' ? '#00d4aa' : (this.categoryColors[m.category] || '#00d4aa');
  }

  getEmoji(m: MarketplaceMerchant): string {
    if (m.source === 'LIVE') return '◎';
    return this.categoryEmojis[m.category] || '◎';
  }

  getTxCount(m: MarketplaceMerchant): string {
    const seed  = `${m.source}-${m.slug}-${m.id}`;
    const base  = m.source === 'LIVE' ? 3   : 120;
    const range = m.source === 'LIVE' ? 420 : 9000;
    return ((this.hash(seed) % range) + base).toLocaleString();
  }

  getSourceLabel(m: MarketplaceMerchant):       string { return m.source === 'LIVE' ? 'LIVE' : 'FEATURED'; }
  getMerchantMetaLabel(m: MarketplaceMerchant): string { return m.source === 'LIVE' ? 'merchant' : 'showcase'; }
  padIndex(index: number):                      string { return String(index + 1).padStart(2, '0'); }

  scrollTo(id: string): void {
    if (id === 'top') { window.scrollTo({ top: 0, behavior: 'smooth' }); return; }
    const el = document.getElementById(id);
    if (el) { const top = el.getBoundingClientRect().top + window.scrollY - 70; window.scrollTo({ top, behavior: 'smooth' }); }
  }

  scrollToMerchants(): void {
    document.getElementById('marketplace')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  goToMyProfile():      void { this.router.navigate(['/profile']);           }
  goToBuilder(slug: string): void { this.router.navigate(['/u', slug]); }
  goToLanding():        void { this.router.navigate(['/']);                  }
  goToLogin():          void { this.router.navigate(['/login']);             }
  goToRegister():       void { this.router.navigate(['/register']);          }
  goToMyPurchases():    void { this.router.navigate(['/buyer/history']);     }
  goToMyStore():        void { this.router.navigate(['/merchant/dashboard']);}
  goToAdminDashboard(): void { this.router.navigate(['/dashboard']);         }

  /** @deprecated Use onPayNow(m.slug). */
  payNow(merchant: MarketplaceMerchant): void {
    if (merchant.source === 'FEATURED') return;
    this.onPayNow(merchant.slug);
  }

  private hash(value: string): number {
    let h = 0;
    for (let i = 0; i < value.length; i++) h = (h * 31 + value.charCodeAt(i)) >>> 0;
    return h;
  }
}
